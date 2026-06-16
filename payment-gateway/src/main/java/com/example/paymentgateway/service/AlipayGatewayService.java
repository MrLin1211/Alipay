package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.CreateWapPayRequest;
import com.example.paymentgateway.domain.CreateWapPayResponse;
import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.util.AlipaySignUtils;
import com.example.paymentgateway.util.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AlipayGatewayService {

    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GatewayJdbcRepository repository;
    private final BusinessNotifyService businessNotifyService;

    public AlipayGatewayService(GatewayJdbcRepository repository, BusinessNotifyService businessNotifyService) {
        this.repository = repository;
        this.businessNotifyService = businessNotifyService;
    }

    public CreateWapPayResponse createWapPay(Map<String, Object> app, CreateWapPayRequest request) {
        String gatewayOrderNo = "GW" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String subject = request.getSubject().trim();
        String totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String businessNotifyUrl = firstNonBlank(string(app.get("notify_url")), request.getBusinessNotifyUrl());

        repository.update(
                """
                        INSERT INTO gateway_pay_order (
                            gateway_order_no, app_id, merchant_order_no, channel, product_code, subject,
                            total_amount, status, business_notify_url, request_payload, created_at, updated_at
                        ) VALUES (?, ?, ?, 'ALIPAY', 'QUICK_WAP_WAY', ?, ?, 'CREATED', ?, ?, NOW(), NOW())
                        """,
                gatewayOrderNo,
                app.get("app_id"),
                request.getMerchantOrderNo(),
                subject,
                totalAmount,
                businessNotifyUrl,
                JsonUtils.toJson(request)
        );

        try {
            Map<String, Object> config = enabledConfig();

            Map<String, String> bizContent = new TreeMap<>();
            bizContent.put("out_trade_no", gatewayOrderNo);
            bizContent.put("total_amount", totalAmount);
            bizContent.put("subject", subject);
            bizContent.put("product_code", "QUICK_WAP_WAY");

            Map<String, String> params = new TreeMap<>();
            params.put("app_id", String.valueOf(config.get("app_id")));
            params.put("method", "alipay.trade.wap.pay");
            params.put("format", "JSON");
            params.put("charset", String.valueOf(config.get("charset_name")));
            params.put("sign_type", String.valueOf(config.get("sign_type")));
            params.put("timestamp", LocalDateTime.now().format(ALIPAY_TIME));
            params.put("version", "1.0");
            params.put("notify_url", String.valueOf(config.get("notify_url")));
            String returnUrl = firstNonBlank(request.getReturnUrl(), String.valueOf(config.get("return_url")));
            if (returnUrl != null && !returnUrl.isBlank() && !"null".equals(returnUrl)) {
                params.put("return_url", returnUrl);
            }
            params.put("biz_content", JsonUtils.toJson(bizContent));
            params.put("sign", AlipaySignUtils.signRsa2(params, String.valueOf(config.get("app_private_key"))));

            String payForm = AlipaySignUtils.formPostHtml(String.valueOf(config.get("gateway_url")), params);

            repository.update(
                    """
                            UPDATE gateway_pay_order
                            SET status = 'PAYING', pay_form = ?, channel_response = ?, updated_at = NOW()
                            WHERE gateway_order_no = ?
                            """,
                    payForm,
                    JsonUtils.toJson(params),
                    gatewayOrderNo
            );
            return new CreateWapPayResponse(gatewayOrderNo, request.getMerchantOrderNo(), payForm);
        } catch (Exception exception) {
            repository.update(
                    """
                            UPDATE gateway_pay_order
                            SET status = 'FAILED', channel_response = ?, updated_at = NOW()
                            WHERE gateway_order_no = ?
                            """,
                    JsonUtils.toJson(Map.of("message", exception.getMessage())),
                    gatewayOrderNo
            );
            throw exception;
        }
    }

    @Transactional
    public boolean handleNotify(Map<String, String> notifyParams) {
        Map<String, Object> config = enabledConfig();
        String sign = notifyParams.get("sign");
        boolean verified = AlipaySignUtils.verifyRsa2(notifyParams, String.valueOf(config.get("alipay_public_key")), sign);
        String notifyKey = buildNotifyKey(notifyParams);
        if (repository.findOne("SELECT id FROM gateway_notify_record WHERE notify_key = ?", notifyKey).isPresent()) {
            return true;
        }

        if (!verified) {
            saveNotify(notifyParams, notifyKey, false, "FAIL", "支付宝通知验签失败");
            return false;
        }

        String gatewayOrderNo = notifyParams.get("out_trade_no");
        String tradeStatus = notifyParams.get("trade_status");
        String status = mapTradeStatus(tradeStatus);

        repository.update(
                """
                        UPDATE gateway_pay_order
                        SET status = ?, alipay_trade_no = ?, trade_status = ?,
                            notify_payload = ?, paid_at = IF(? = 'SUCCESS', NOW(), paid_at), updated_at = NOW()
                        WHERE gateway_order_no = ?
                        """,
                status,
                notifyParams.get("trade_no"),
                tradeStatus,
                JsonUtils.toJson(new TreeMap<>(notifyParams)),
                status,
                gatewayOrderNo
        );
        saveNotify(notifyParams, notifyKey, true, "SUCCESS", null);
        businessNotifyService.notifyBusiness(gatewayOrderNo);
        return true;
    }

    public Map<String, Object> getConfig() {
        return repository.findOne("SELECT * FROM alipay_channel_config ORDER BY id LIMIT 1").orElse(Map.of());
    }

    public void saveConfig(Map<String, Object> payload) {
        Integer count = repository.jdbc().queryForObject("SELECT COUNT(*) FROM alipay_channel_config", Integer.class);
        if (count == null || count == 0) {
            repository.update(
                    """
                            INSERT INTO alipay_channel_config (
                                config_name, app_id, gateway_url, app_private_key, alipay_public_key,
                                notify_url, return_url, sign_type, charset_name, enabled, sandbox, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'RSA2', 'utf-8', ?, ?, NOW(), NOW())
                            """,
                    value(payload, "configName", "default"),
                    value(payload, "appId", ""),
                    value(payload, "gatewayUrl", "https://openapi.alipay.com/gateway.do"),
                    value(payload, "appPrivateKey", ""),
                    value(payload, "alipayPublicKey", ""),
                    value(payload, "notifyUrl", ""),
                    value(payload, "returnUrl", ""),
                    boolValue(payload, "enabled"),
                    boolValue(payload, "sandbox")
            );
            return;
        }
        Map<String, Object> current = getConfig();
        repository.update(
                """
                        UPDATE alipay_channel_config
                        SET config_name = ?, app_id = ?, gateway_url = ?, app_private_key = ?,
                            alipay_public_key = ?, notify_url = ?, return_url = ?,
                            enabled = ?, sandbox = ?, updated_at = NOW()
                        ORDER BY id LIMIT 1
                        """,
                value(payload, "configName", "default"),
                value(payload, "appId", ""),
                value(payload, "gatewayUrl", "https://openapi.alipay.com/gateway.do"),
                valueOrCurrent(payload, current, "appPrivateKey", "app_private_key"),
                valueOrCurrent(payload, current, "alipayPublicKey", "alipay_public_key"),
                value(payload, "notifyUrl", ""),
                value(payload, "returnUrl", ""),
                boolValue(payload, "enabled"),
                boolValue(payload, "sandbox")
        );
    }

    private Map<String, Object> enabledConfig() {
        return repository.findOne("SELECT * FROM alipay_channel_config WHERE enabled = true ORDER BY id LIMIT 1")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付宝渠道未启用或未配置"));
    }

    private void saveNotify(Map<String, String> params, String notifyKey, boolean verified, String result, String reason) {
        repository.update(
                """
                        INSERT INTO gateway_notify_record (
                            gateway_order_no, merchant_order_no, channel, channel_trade_no, trade_status,
                            notify_key, verified, result, failure_reason, notify_payload, created_at, updated_at
                        ) VALUES (?, ?, 'ALIPAY', ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                params.get("out_trade_no"),
                params.get("out_biz_no"),
                params.get("trade_no"),
                params.get("trade_status"),
                notifyKey,
                verified,
                result,
                reason,
                JsonUtils.toJson(new TreeMap<>(params))
        );
    }

    private static String mapTradeStatus(String tradeStatus) {
        if ("TRADE_SUCCESS".equals(tradeStatus)) return "SUCCESS";
        if ("TRADE_FINISHED".equals(tradeStatus)) return "FINISHED";
        if ("TRADE_CLOSED".equals(tradeStatus)) return "CLOSED";
        return "UNKNOWN";
    }

    private static String buildNotifyKey(Map<String, String> params) {
        return firstNonBlank(params.get("out_trade_no"), "-") + "|"
                + firstNonBlank(params.get("trade_no"), "-") + "|"
                + firstNonBlank(params.get("trade_status"), "-");
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private static String value(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean boolValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static String valueOrCurrent(Map<String, Object> payload, Map<String, Object> current, String payloadKey, String currentKey) {
        Object value = payload.get(payloadKey);
        if (value != null && !String.valueOf(value).isBlank()) {
            return String.valueOf(value);
        }
        Object currentValue = current.get(currentKey);
        return currentValue == null ? "" : String.valueOf(currentValue);
    }
}
