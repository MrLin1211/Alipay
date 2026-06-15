package com.example.mallhome.service;

import com.example.mallhome.client.MallhomePayClient;
import com.example.mallhome.config.MallhomePayProperties;
import com.example.mallhome.domain.CreatePayOrderRequest;
import com.example.mallhome.domain.CreatePayOrderResponse;
import com.example.mallhome.domain.PaymentOrderStatus;
import com.example.mallhome.entity.PaymentOrder;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.util.ClientIpUtils;
import com.example.mallhome.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.TreeMap;

@Service
public class PayOrderService {

    private final MallhomePayProperties properties;
    private final MallhomePayClient payClient;
    private final PaymentOrderRepository paymentOrderRepository;
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int ORDER_CREATE_MAX_RETRY = 5;

    public PayOrderService(
            MallhomePayProperties properties,
            MallhomePayClient payClient,
            PaymentOrderRepository paymentOrderRepository
    ) {
        this.properties = properties;
        this.payClient = payClient;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    public CreatePayOrderResponse createPayOrder(CreatePayOrderRequest request, HttpServletRequest servletRequest) {
        String clientIp = ClientIpUtils.resolveClientIp(servletRequest);
        PaymentOrder order = createAndSaveLocalOrder(request, clientIp);
        String orderNo = order.getOrderNo();

        // 组装平台支付接口要求的表单参数，字段名需要和接口文档保持一致。
        Map<String, String> params = new TreeMap<>();
        params.put("merTradeNo", orderNo);
        params.put("typeIndex", String.valueOf(request.getTypeIndex()));
        params.put("externalId", properties.getExternalId());
        params.put("totalAmount", request.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("merSubject", request.getSubject());
        params.put("goodsType", String.valueOf(request.getGoodsType()));
        params.put("merPayNotifyUrl", properties.getNotifyUrl());
        params.put("clientIp", clientIp);
        params.put("payMethodType", firstNonBlank(request.getPayMethodType(), properties.getDefaultPayMethodType()));

        putIfHasText(params, "attachInfo", request.getAttachInfo());
        putIfHasText(params, "quitUrl", request.getQuitUrl());
        putIfHasText(params, "returnUrl", firstNonBlank(request.getReturnUrl(), properties.getReturnUrl()));
        putIfHasText(params, "subExternalId", request.getSubExternalId());

        try {
            String rawResponse = payClient.operPay(params);
            // 平台创建成功后，把 payUrl、平台单号等关键信息回写到本地订单表。
            applyCreateResponse(order, rawResponse);
            paymentOrderRepository.save(order);
            return new CreatePayOrderResponse(
                    order.getId(),
                    order.getOrderNo(),
                    order.getStatus(),
                    order.getPayUrl(),
                    order.getPlatTradeNo(),
                    rawResponse
            );
        } catch (RuntimeException exception) {
            // 调用平台异常时也记录失败状态，便于后台排查和前端查询。
            order.setStatus(PaymentOrderStatus.CREATE_FAILED);
            order.setPlatformCreateResponse(exception.getMessage());
            paymentOrderRepository.save(order);
            throw exception;
        }
    }

    private PaymentOrder createAndSaveLocalOrder(CreatePayOrderRequest request, String clientIp) {
        for (int i = 0; i < ORDER_CREATE_MAX_RETRY; i++) {
            String orderNo = generateOrderNo();
            PaymentOrder order = createLocalOrder(request, orderNo, clientIp);
            try {
                // 先保存本地订单，再调用平台。数据库唯一键兜底订单号并发撞号。
                return paymentOrderRepository.saveAndFlush(order);
            } catch (DataIntegrityViolationException exception) {
                if (i == ORDER_CREATE_MAX_RETRY - 1) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("商户订单号生成失败，请重试");
    }

    private PaymentOrder createLocalOrder(CreatePayOrderRequest request, String orderNo, String clientIp) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setExternalId(properties.getExternalId());
        order.setTotalAmount(request.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        order.setSubject(request.getSubject());
        order.setClientIp(clientIp);
        order.setTypeIndex(request.getTypeIndex());
        order.setGoodsType(request.getGoodsType());
        order.setPayMethodType(firstNonBlank(request.getPayMethodType(), properties.getDefaultPayMethodType()));
        order.setAttachInfo(request.getAttachInfo());
        order.setReturnUrl(firstNonBlank(request.getReturnUrl(), properties.getReturnUrl()));
        order.setQuitUrl(request.getQuitUrl());
        order.setSubExternalId(request.getSubExternalId());
        order.setStatus(PaymentOrderStatus.CREATED);
        return order;
    }

    private String generateOrderNo() {
        for (int i = 0; i < 5; i++) {
            String orderNo = "ORDER"
                    + LocalDateTime.now().format(ORDER_TIME_FORMATTER)
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (paymentOrderRepository.findByOrderNo(orderNo).isEmpty()) {
                return orderNo;
            }
        }
        throw new IllegalStateException("商户订单号生成失败，请重试");
    }

    private void applyCreateResponse(PaymentOrder order, String rawResponse) {
        // 保存平台原始响应，后续出现对账或排查问题时可以还原现场。
        order.setPlatformCreateResponse(rawResponse);

        JsonNode root = JsonUtils.readTree(rawResponse);
        if (root.path("code").asInt(-1) != 0) {
            order.setStatus(PaymentOrderStatus.CREATE_FAILED);
            return;
        }

        JsonNode payData = root.path("data").path("data");
        order.setPayUrl(payData.path("payUrl").asText(null));
        order.setEvokeMode(payData.path("evokeMode").asText(null));
        order.setPlatTradeNo(payData.path("platTradeNo").asText(null));
        order.setStatus(PaymentOrderStatus.CREATE_SUCCESS);
    }

    private static void putIfHasText(Map<String, String> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
