package com.example.mallhome.service;

import com.example.mallhome.config.MallhomePayProperties;
import com.example.mallhome.domain.PayNotifyResult;
import com.example.mallhome.domain.PaymentOrderStatus;
import com.example.mallhome.entity.PaymentNotifyRecord;
import com.example.mallhome.entity.PaymentOrder;
import com.example.mallhome.repository.PaymentNotifyRecordRepository;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.util.JsonUtils;
import com.example.mallhome.util.MallhomeSignUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class PayNotifyService {

    private static final Logger log = LoggerFactory.getLogger(PayNotifyService.class);

    private final MallhomePayProperties properties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentNotifyRecordRepository notifyRecordRepository;

    public PayNotifyService(
            MallhomePayProperties properties,
            PaymentOrderRepository paymentOrderRepository,
            PaymentNotifyRecordRepository notifyRecordRepository
    ) {
        this.properties = properties;
        this.paymentOrderRepository = paymentOrderRepository;
        this.notifyRecordRepository = notifyRecordRepository;
    }

    @Transactional
    public PayNotifyResult handleNotify(String timeStamp, String visitAuth, Map<String, String> notifyParams) {
        // 平台通知必须同时带上时间戳和 visitAuth，用于确认通知来源。
        if (!StringUtils.hasText(timeStamp) || !StringUtils.hasText(visitAuth)) {
            return failNotify(notifyParams, false, "missing timeStamp or visitAuth");
        }

        // 先校验 Header 里的 visitAuth，再校验 Body 里的业务签名。
        if (!MallhomeSignUtils.verifyVisitAuth(properties.getMd5Key(), properties.getAesKey(), timeStamp, visitAuth)) {
            return failNotify(notifyParams, false, "invalid visitAuth");
        }

        String pltNotifySign = notifyParams.get("pltNotifySign");
        if (!MallhomeSignUtils.verifyNotifySign(notifyParams, visitAuth, properties.getAesKey(), pltNotifySign)) {
            return failNotify(notifyParams, false, "invalid pltNotifySign");
        }

        // 防止其他商户号的通知误更新当前系统订单。
        if (!properties.getExternalId().equals(notifyParams.get("externalId"))) {
            return failNotify(notifyParams, true, "externalId mismatch");
        }

        log.info("payment notify verified: {}", maskSensitive(new TreeMap<>(notifyParams)));

        // 平台回调中的 merchantTradeNo 对应本地创建订单时的 orderNo。
        String orderNo = notifyParams.get("merchantTradeNo");
        String notifyKey = buildNotifyKey(notifyParams);
        if (notifyRecordRepository.existsByNotifyKey(notifyKey)) {
            log.info("duplicate payment notify ignored: {}", notifyKey);
            return PayNotifyResult.success();
        }

        PaymentOrder order = paymentOrderRepository.findWithLockByOrderNo(orderNo)
                .orElse(null);
        if (order == null) {
            return failNotify(notifyParams, true, "local order not found: " + orderNo);
        }
        if (notifyRecordRepository.existsByNotifyKey(notifyKey)) {
            log.info("duplicate payment notify ignored after order lock: {}", notifyKey);
            return PayNotifyResult.success();
        }

        // 验签通过后才更新订单状态，避免伪造通知污染本地订单。
        String incomingStatus = PaymentOrderStatus.fromTradeStatus(notifyParams.get("tradeStatus"));
        if (canApplyStatus(order.getStatus(), incomingStatus)) {
            order.setTradeStatus(notifyParams.get("tradeStatus"));
            order.setStatus(incomingStatus);
            order.setPlatTradeNo(firstNonBlank(notifyParams.get("platformOutTradeNo"), order.getPlatTradeNo()));
            order.setThirdOutTradeNo(notifyParams.get("thirdOutTradeNo"));
            order.setNotifyPayload(JsonUtils.toJson(new TreeMap<>(notifyParams)));
            if (PaymentOrderStatus.SUCCESS.equals(incomingStatus)) {
                order.setPaidAt(parseNotifyTime(notifyParams.get("sucTime")));
            }
            paymentOrderRepository.save(order);
        } else {
            log.info("payment notify status ignored: orderNo={}, currentStatus={}, incomingStatus={}",
                    orderNo, order.getStatus(), incomingStatus);
        }

        if (!saveNotifyRecord(notifyParams, true, "SUCCESS", null)) {
            log.info("duplicate payment notify ignored after unique check: {}", notifyKey);
        }

        return PayNotifyResult.success();
    }

    private PayNotifyResult failNotify(Map<String, String> notifyParams, boolean verified, String reason) {
        saveNotifyRecord(notifyParams, verified, "FAIL", reason);
        return PayNotifyResult.fail(reason);
    }

    private boolean saveNotifyRecord(Map<String, String> notifyParams, boolean verified, String result, String failureReason) {
        PaymentNotifyRecord record = new PaymentNotifyRecord();
        record.setOrderNo(notifyParams.get("merchantTradeNo"));
        record.setExternalId(notifyParams.get("externalId"));
        record.setTradeStatus(notifyParams.get("tradeStatus"));
        record.setPlatformOutTradeNo(notifyParams.get("platformOutTradeNo"));
        record.setNotifyKey(buildNotifyKey(notifyParams));
        record.setVerified(verified);
        record.setResult(result);
        record.setFailureReason(failureReason);
        record.setNotifyPayload(JsonUtils.toJson(new TreeMap<>(notifyParams)));
        try {
            notifyRecordRepository.saveAndFlush(record);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private static boolean canApplyStatus(String currentStatus, String incomingStatus) {
        return statusPriority(incomingStatus) >= statusPriority(currentStatus);
    }

    private static int statusPriority(String status) {
        if (PaymentOrderStatus.SUCCESS.equals(status)) {
            return 40;
        }
        if (PaymentOrderStatus.FINISHED.equals(status) || PaymentOrderStatus.CLOSED.equals(status)) {
            return 30;
        }
        if (PaymentOrderStatus.CREATE_SUCCESS.equals(status)) {
            return 20;
        }
        if (PaymentOrderStatus.CREATED.equals(status) || PaymentOrderStatus.CREATE_FAILED.equals(status)) {
            return 10;
        }
        return 0;
    }

    private static String buildNotifyKey(Map<String, String> notifyParams) {
        if (!StringUtils.hasText(notifyParams.get("merchantTradeNo"))) {
            return "INVALID|" + UUID.randomUUID();
        }
        return firstNonBlank(notifyParams.get("merchantTradeNo"), "-")
                + "|"
                + firstNonBlank(notifyParams.get("platformOutTradeNo"), "-")
                + "|"
                + firstNonBlank(notifyParams.get("tradeStatus"), "-");
    }

    private static Map<String, String> maskSensitive(Map<String, String> params) {
        // 日志里保留排查所需字段，但隐藏签名和买家信息。
        params.computeIfPresent("pltNotifySign", (key, value) -> "******");
        params.computeIfPresent("buyerInfo", (key, value) -> "******");
        return params;
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static LocalDateTime parseNotifyTime(String value) {
        // 平台时间格式可能有差异，这里兼容常见格式，解析失败则不写 paidAt。
        if (!StringUtils.hasText(value)) {
            return null;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        }) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
