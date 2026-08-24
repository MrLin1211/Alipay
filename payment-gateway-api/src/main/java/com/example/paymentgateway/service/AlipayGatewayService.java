package com.example.paymentgateway.service;

import com.example.paymentgateway.domain.CreateWapPayRequest;
import com.example.paymentgateway.domain.CreateWapPayResponse;
import com.example.paymentgateway.domain.RefundRequest;
import com.example.paymentgateway.domain.TradeQueryRequest;
import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.util.JsonUtils;
import com.example.paymentgateway.util.ZhenbaogeSignUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AlipayGatewayService {

    private final GatewayJdbcRepository repository;
    private final BusinessNotifyService businessNotifyService;
    private final ZhenbaogePayClient zhenbaogePayClient;

    public AlipayGatewayService(GatewayJdbcRepository repository, BusinessNotifyService businessNotifyService,
                                ZhenbaogePayClient zhenbaogePayClient) {
        this.repository = repository;
        this.businessNotifyService = businessNotifyService;
        this.zhenbaogePayClient = zhenbaogePayClient;
    }

    public CreateWapPayResponse createWapPay(Map<String, Object> app, CreateWapPayRequest request) {
        int typeIndex = request.getTypeIndex() == null ? 1 : request.getTypeIndex();
        if (typeIndex == 1 && !StringUtils.hasText(request.getQuitUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机网站支付必须提供中途退出地址");
        }
        String gatewayOrderNo = "GW" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String subject = request.getSubject().trim();
        String totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String businessNotifyUrl = firstNonBlank(string(app.get("notify_url")), request.getBusinessNotifyUrl());

        repository.update(
                """
                        INSERT INTO gateway_pay_order (
                            gateway_order_no, app_id, merchant_order_no, channel, product_code, subject,
                            total_amount, status, return_url, business_notify_url, request_payload, created_at, updated_at
                        ) VALUES (?, ?, ?, 'ZHENBAOGE', 'OPER_PAY', ?, ?, 'CREATED', ?, ?, ?, NOW(), NOW())
                        """,
                gatewayOrderNo,
                app.get("app_id"),
                request.getMerchantOrderNo(),
                subject,
                totalAmount,
                request.getReturnUrl(),
                businessNotifyUrl,
                JsonUtils.toJson(request)
        );

        try {
            Map<String, Object> config = enabledConfig();
            Map<String, Object> params = buildPayParams(config, gatewayOrderNo, request, subject, totalAmount);
            JsonNode response = zhenbaogePayClient.createPay(config, params);
            ensurePlatformSuccess(response);
            JsonNode data = response.path("data").path("data");
            String payUrl = text(data, "payUrl");
            String evokeMode = text(data, "evokeMode");
            String platformTradeNo = text(data, "platTradeNo");
            String payForm = buildPayPage(payUrl, evokeMode);

            repository.update(
                    """
                            UPDATE gateway_pay_order
                            SET status = 'PAYING', platform_trade_no = ?, pay_form = ?, channel_response = ?, updated_at = NOW()
                            WHERE gateway_order_no = ?
                            """,
                    platformTradeNo,
                    payForm,
                    JsonUtils.toJson(response),
                    gatewayOrderNo
            );
            return new CreateWapPayResponse(gatewayOrderNo, request.getMerchantOrderNo(), payForm, payUrl, evokeMode, platformTradeNo);
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
    public boolean handleNotify(Map<String, String> notifyParams, String timestamp, String visitAuth) {
        Map<String, Object> config = enabledConfig();
        boolean verified = verifyPlatformNotify(config, notifyParams, timestamp, visitAuth);
        String notifyKey = buildNotifyKey(notifyParams);

        Map<String, Object> existing = repository
                .findOne("SELECT verified, result FROM gateway_notify_record WHERE notify_key = ?", notifyKey)
                .orElse(null);
        if (existing != null && isTrue(existing.get("verified"))
                && "SUCCESS".equals(String.valueOf(existing.get("result")))) {
            return true;
        }

        if (!verified) {
            saveNotify(notifyParams, notifyKey, false, "FAIL", "平台通知验签失败");
            return false;
        }

        String gatewayOrderNo = firstNonBlank(notifyParams.get("merchantTradeNo"), notifyParams.get("merTradeNo"));
        String tradeStatus = notifyParams.get("trade_status");
        if (!StringUtils.hasText(tradeStatus)) {
            tradeStatus = notifyParams.get("tradeStatus");
        }
        String status = mapTradeStatus(tradeStatus);

        repository.update(
                """
                        UPDATE gateway_pay_order
                        SET status = ?, platform_trade_no = ?, alipay_trade_no = ?, trade_status = ?,
                            notify_payload = ?, paid_at = IF(? = 'SUCCESS', NOW(), paid_at), updated_at = NOW()
                        WHERE gateway_order_no = ?
                        """,
                status,
                firstNonBlank(notifyParams.get("platformOutTradeNo"), notifyParams.get("platTradeNo")),
                firstNonBlank(notifyParams.get("thirdOutTradeNo"), notifyParams.get("tradeNo")),
                tradeStatus,
                JsonUtils.toJson(new TreeMap<>(notifyParams)),
                status,
                gatewayOrderNo
        );
        saveNotify(notifyParams, notifyKey, true, "SUCCESS", null);
        businessNotifyService.notifyBusiness(gatewayOrderNo);
        return true;
    }

    public Map<String, Object> queryTrade(Map<String, Object> app, TradeQueryRequest request) {
        Map<String, Object> order = findOrder(app, request.getGatewayOrderNo(), request.getMerchantOrderNo());
        Map<String, Object> config = enabledConfig();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("platTradeNo", requiredString(order.get("platform_trade_no"), "订单缺少平台单号，无法查询"));
        params.put("externalId", config.get("external_id"));
        JsonNode response = zhenbaogePayClient.queryTrade(config, params);
        return Map.of("order", order, "platformResponse", response);
    }

    public Map<String, Object> refund(Map<String, Object> app, RefundRequest request) {
        Map<String, Object> order = findOrder(app, request.getGatewayOrderNo(), null);
        Map<String, Object> config = enabledConfig();
        String refundOrderNo = "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("platTradeNo", requiredString(order.get("platform_trade_no"), "订单缺少平台单号，无法退款"));
        params.put("refundAmount", request.getRefundAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("externalId", config.get("external_id"));
        params.put("refundReason", request.getRefundReason());

        repository.update(
                """
                        INSERT INTO gateway_refund_order (
                            refund_order_no, gateway_order_no, app_id, merchant_order_no, platform_trade_no,
                            refund_amount, refund_reason, status, request_payload, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'CREATED', ?, NOW(), NOW())
                        """,
                refundOrderNo,
                order.get("gateway_order_no"),
                app.get("app_id"),
                order.get("merchant_order_no"),
                order.get("platform_trade_no"),
                request.getRefundAmount(),
                request.getRefundReason(),
                JsonUtils.toJson(params)
        );

        try {
            JsonNode response = zhenbaogePayClient.refund(config, params);
            String status = response.path("code").asInt(-1) == 0 ? "SUCCESS" : "FAILED";
            String tradeStatus = text(response.path("data"), "tradeStatus");
            repository.update(
                    """
                            UPDATE gateway_refund_order
                            SET status = ?, trade_status = ?, platform_response = ?, updated_at = NOW()
                            WHERE refund_order_no = ?
                            """,
                    status,
                    tradeStatus,
                    JsonUtils.toJson(response),
                    refundOrderNo
            );
            return Map.of("refundOrderNo", refundOrderNo, "status", status, "platformResponse", response);
        } catch (Exception exception) {
            repository.update(
                    """
                            UPDATE gateway_refund_order
                            SET status = 'FAILED', platform_response = ?, updated_at = NOW()
                            WHERE refund_order_no = ?
                            """,
                    JsonUtils.toJson(Map.of("message", exception.getMessage())),
                    refundOrderNo
            );
            throw exception;
        }
    }

    public Map<String, Object> getConfig() {
        return repository.findOne(
                """
                        SELECT id, config_name, host, external_id, notify_url, enabled, created_at, updated_at,
                               (md5_key IS NOT NULL AND md5_key <> '') AS md5_configured,
                               (aes_key IS NOT NULL AND aes_key <> '') AS aes_configured
                        FROM zhenbaoge_channel_config
                        ORDER BY id LIMIT 1
                        """
        ).orElse(Map.of());
    }

    public void saveConfig(Map<String, Object> payload) {
        Integer count = repository.jdbc().queryForObject("SELECT COUNT(*) FROM zhenbaoge_channel_config", Integer.class);
        if (count == null || count == 0) {
            repository.update(
                    """
                            INSERT INTO zhenbaoge_channel_config (
                                config_name, host, external_id, md5_key, aes_key,
                                notify_url, enabled, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                            """,
                    value(payload, "configName", "default"),
                    value(payload, "host", "https://pay.zhenbaoge.com"),
                    value(payload, "externalId", ""),
                    value(payload, "md5Key", ""),
                    value(payload, "aesKey", ""),
                    value(payload, "notifyUrl", ""),
                    boolValue(payload, "enabled")
            );
            return;
        }
        Map<String, Object> current = repository
                .findOne("SELECT * FROM zhenbaoge_channel_config ORDER BY id LIMIT 1")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "支付平台通道配置不存在"));
        repository.update(
                """
                        UPDATE zhenbaoge_channel_config
                        SET config_name = ?, host = ?, external_id = ?, md5_key = ?,
                            aes_key = ?, notify_url = ?, enabled = ?, updated_at = NOW()
                        ORDER BY id LIMIT 1
                        """,
                value(payload, "configName", "default"),
                value(payload, "host", "https://pay.zhenbaoge.com"),
                value(payload, "externalId", ""),
                valueOrCurrent(payload, current, "md5Key", "md5_key"),
                valueOrCurrent(payload, current, "aesKey", "aes_key"),
                value(payload, "notifyUrl", ""),
                boolValue(payload, "enabled")
        );
    }

    private Map<String, Object> enabledConfig() {
        return repository.findOne("SELECT * FROM zhenbaoge_channel_config WHERE enabled = true ORDER BY id LIMIT 1")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付平台通道未启用或未配置"));
    }

    private void saveNotify(Map<String, String> params, String notifyKey, boolean verified, String result, String reason) {
        String gatewayOrderNo = firstNonBlank(params.get("merchantTradeNo"), params.get("merTradeNo"));
        String merchantOrderNo = resolveMerchantOrderNo(params, gatewayOrderNo);
        String platformTradeNo = resolvePlatformTradeNo(params, gatewayOrderNo);
        repository.update(
                """
                        INSERT INTO gateway_notify_record (
                            gateway_order_no, merchant_order_no, platform_trade_no, channel, channel_trade_no, trade_status,
                            notify_key, verified, result, failure_reason, notify_payload, created_at, updated_at
                        ) VALUES (?, ?, ?, 'ZHENBAOGE', ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        ON DUPLICATE KEY UPDATE
                            gateway_order_no = VALUES(gateway_order_no),
                            merchant_order_no = VALUES(merchant_order_no),
                            platform_trade_no = VALUES(platform_trade_no),
                            channel_trade_no = VALUES(channel_trade_no),
                            trade_status = VALUES(trade_status),
                            verified = VALUES(verified),
                            result = VALUES(result),
                            failure_reason = VALUES(failure_reason),
                            notify_payload = VALUES(notify_payload),
                            updated_at = NOW()
                        """,
                gatewayOrderNo,
                merchantOrderNo,
                platformTradeNo,
                firstNonBlank(params.get("thirdOutTradeNo"), params.get("tradeNo")),
                firstNonBlank(params.get("tradeStatus"), params.get("trade_status")),
                notifyKey,
                verified,
                result,
                reason,
                JsonUtils.toJson(new TreeMap<>(params))
        );
    }

    String resolveMerchantOrderNo(Map<String, String> params, String gatewayOrderNo) {
        String callbackOrderNo = firstNonBlank(params.get("merchantOrderNo"), params.get("out_biz_no"));
        if (StringUtils.hasText(callbackOrderNo) || !StringUtils.hasText(gatewayOrderNo)) {
            return callbackOrderNo;
        }
        return repository.findOne(
                        "SELECT merchant_order_no FROM gateway_pay_order WHERE gateway_order_no = ?",
                        gatewayOrderNo
                )
                .map(order -> string(order.get("merchant_order_no")))
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    String resolvePlatformTradeNo(Map<String, String> params, String gatewayOrderNo) {
        String callbackPlatformTradeNo = firstNonBlank(params.get("platformOutTradeNo"), params.get("platTradeNo"));
        if (StringUtils.hasText(callbackPlatformTradeNo) || !StringUtils.hasText(gatewayOrderNo)) {
            return callbackPlatformTradeNo;
        }
        return repository.findOne(
                        "SELECT platform_trade_no FROM gateway_pay_order WHERE gateway_order_no = ?",
                        gatewayOrderNo
                )
                .map(order -> string(order.get("platform_trade_no")))
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private static String mapTradeStatus(String tradeStatus) {
        if ("TRADE_SUCCESS".equals(tradeStatus)) return "SUCCESS";
        if ("WAIT_BUYER_PAY".equals(tradeStatus)) return "PAYING";
        if ("TRADE_REFUND_SUCCESS".equals(tradeStatus)) return "REFUNDED";
        if ("TRADE_FINISHED".equals(tradeStatus)) return "FINISHED";
        if ("TRADE_CLOSED".equals(tradeStatus)) return "CLOSED";
        return "UNKNOWN";
    }

    private static String buildNotifyKey(Map<String, String> params) {
        return firstNonBlank(params.get("platformOutTradeNo"), firstNonBlank(params.get("platTradeNo"), "-")) + "|"
                + firstNonBlank(params.get("thirdOutTradeNo"), firstNonBlank(params.get("tradeNo"), "-")) + "|"
                + firstNonBlank(params.get("tradeStatus"), firstNonBlank(params.get("trade_status"), "-"));
    }

    private Map<String, Object> buildPayParams(Map<String, Object> config, String gatewayOrderNo,
                                               CreateWapPayRequest request, String subject, String totalAmount) {
        String notifyUrl = requiredString(config.get("notify_url"), "请先配置支付平台异步通知地址");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("merTradeNo", gatewayOrderNo);
        params.put("typeIndex", request.getTypeIndex() == null ? 1 : request.getTypeIndex());
        params.put("externalId", requiredString(config.get("external_id"), "请先配置支付平台商户ID"));
        params.put("totalAmount", totalAmount);
        params.put("merSubject", subject);
        params.put("goodsType", request.getGoodsType() == null ? 1 : request.getGoodsType());
        params.put("merPayNotifyUrl", notifyUrl);
        params.put("clientIp", normalizeClientIp(request.getClientIp()));
        params.put("payMethodType", firstNonBlank(request.getPayMethodType(), "ALIPAY_CN"));
        putIfHasText(params, "subExternalId", request.getSubExternalId());
        putIfHasText(params, "attachInfo", firstNonBlank(request.getAttachInfo(), request.getMerchantOrderNo()));
        putIfHasText(params, "quitUrl", request.getQuitUrl());
        putIfHasText(params, "returnUrl", request.getReturnUrl());
        putIfNotNull(params, "isShort", request.getIsShort());
        putIfNotNull(params, "isQr", request.getIsQr());
        return params;
    }

    private boolean verifyPlatformNotify(Map<String, Object> config, Map<String, String> params,
                                         String timestamp, String visitAuth) {
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(visitAuth)) {
            return false;
        }
        String md5Key = requiredString(config.get("md5_key"), "请先配置支付平台MD5密钥");
        String aesKey = requiredString(config.get("aes_key"), "请先配置支付平台AES密钥");
        String expectedVisitAuth = ZhenbaogeSignUtils.visitAuth(md5Key, aesKey, timestamp);
        return expectedVisitAuth.equals(visitAuth) && ZhenbaogeSignUtils.verifyNotify(params, visitAuth, aesKey);
    }

    private Map<String, Object> findOrder(Map<String, Object> app, String gatewayOrderNo, String merchantOrderNo) {
        if (StringUtils.hasText(gatewayOrderNo)) {
            return repository.findOne(
                    "SELECT * FROM gateway_pay_order WHERE gateway_order_no = ? AND app_id = ?",
                    gatewayOrderNo,
                    app.get("app_id")
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        }
        if (StringUtils.hasText(merchantOrderNo)) {
            return repository.findOne(
                    "SELECT * FROM gateway_pay_order WHERE merchant_order_no = ? AND app_id = ?",
                    merchantOrderNo,
                    app.get("app_id")
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gatewayOrderNo和merchantOrderNo至少传一个");
    }

    private static void ensurePlatformSuccess(JsonNode response) {
        if (response.path("code").asInt(-1) != 0) {
            String message = response.path("msg").asText(response.path("message").asText("支付平台请求失败"));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        }
    }

    private static String buildPayPage(String payUrl, String evokeMode) {
        if (!StringUtils.hasText(payUrl)) {
            return "";
        }
        if ("3".equals(evokeMode)) {
            return "";
        }
        if ("1".equals(evokeMode) && payUrl.trim().startsWith("<")) {
            return payUrl;
        }
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>支付跳转</title></head>"
                + "<body><script>location.replace('" + escapeJs(payUrl) + "');</script>"
                + "<a href=\"" + escapeHtml(payUrl) + "\">继续支付</a></body></html>";
    }

    private static String normalizeClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp) || "127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)
                || "::1".equals(clientIp)) {
            return "1.1.1.1";
        }
        return clientIp;
    }

    private static void putIfHasText(Map<String, Object> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private static void putIfNotNull(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private static String requiredString(Object value, String message) {
        String text = string(value);
        if (!StringUtils.hasText(text)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text;
    }

    private static String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeJs(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
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

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
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
