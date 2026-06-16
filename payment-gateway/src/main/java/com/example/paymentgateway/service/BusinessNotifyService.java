package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.util.HmacUtils;
import com.example.paymentgateway.util.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class BusinessNotifyService {

    private final GatewayJdbcRepository repository;
    private final RestClient.Builder builder;

    public BusinessNotifyService(GatewayJdbcRepository repository, RestClient.Builder builder) {
        this.repository = repository;
        this.builder = builder;
    }

    public void notifyBusiness(String gatewayOrderNo) {
        Map<String, Object> order = repository.findOne(
                """
                        SELECT o.*, a.app_secret
                        FROM gateway_pay_order o
                        JOIN gateway_app a ON a.app_id = o.app_id
                        WHERE o.gateway_order_no = ?
                        """,
                gatewayOrderNo
        ).orElse(null);
        if (order == null) {
            return;
        }

        String notifyUrl = string(order.get("business_notify_url"));
        if (!StringUtils.hasText(notifyUrl)) {
            repository.update(
                    "UPDATE gateway_pay_order SET business_notify_result = 'SKIPPED', business_notified_at = NOW(), updated_at = NOW() WHERE gateway_order_no = ?",
                    gatewayOrderNo
            );
            return;
        }

        Map<String, Object> payload = new TreeMap<>();
        payload.put("merchantOrderNo", order.get("merchant_order_no"));
        payload.put("gatewayOrderNo", order.get("gateway_order_no"));
        payload.put("appId", order.get("app_id"));
        payload.put("channel", order.get("channel"));
        payload.put("status", order.get("status"));
        payload.put("tradeStatus", order.get("trade_status"));
        payload.put("alipayTradeNo", order.get("alipay_trade_no"));
        payload.put("totalAmount", order.get("total_amount"));
        payload.put("paidAt", order.get("paid_at"));
        payload.put("notifyPayload", order.get("notify_payload"));

        String body = JsonUtils.toJson(payload);
        String timestamp = LocalDateTime.now().toString();
        String nonce = UUID.randomUUID().toString();
        String path = URI.create(notifyUrl).getRawPath();
        String signText = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = HmacUtils.hmacSha256Base64(string(order.get("app_secret")), signText);

        try {
            String result = builder.build()
                    .post()
                    .uri(notifyUrl)
                    .header("X-Gateway-App-Id", string(order.get("app_id")))
                    .header("X-Gateway-Timestamp", timestamp)
                    .header("X-Gateway-Nonce", nonce)
                    .header("X-Gateway-Signature", signature)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            repository.update(
                    """
                            UPDATE gateway_pay_order
                            SET business_notify_result = ?, business_notified_at = NOW(), updated_at = NOW()
                            WHERE gateway_order_no = ?
                            """,
                    "success".equalsIgnoreCase(result) ? "SUCCESS" : "FAIL",
                    gatewayOrderNo
            );
        } catch (Exception exception) {
            repository.update(
                    """
                            UPDATE gateway_pay_order
                            SET business_notify_result = 'FAIL', business_notified_at = NOW(), updated_at = NOW()
                            WHERE gateway_order_no = ?
                            """,
                    gatewayOrderNo
            );
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
