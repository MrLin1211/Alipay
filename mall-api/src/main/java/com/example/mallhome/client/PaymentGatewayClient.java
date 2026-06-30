package com.example.mallhome.client;

import com.example.mallhome.util.HmacUtils;
import com.example.mallhome.util.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentGatewayClient {

    private static final String WAP_PAY_PATH = "/api/gateway/pay/alipay/wap";

    private final RestClient.Builder builder;

    public PaymentGatewayClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    public String createWapPay(String gatewayHost, String appId, String appSecret, Map<String, Object> payload) {
        String body = JsonUtils.toJson(payload);
        String timestamp = LocalDateTime.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signText = "POST\n" + WAP_PAY_PATH + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = HmacUtils.hmacSha256Base64(appSecret, signText);

        return builder.baseUrl(gatewayHost).build()
                .post()
                .uri(WAP_PAY_PATH)
                .header("X-Gateway-App-Id", appId)
                .header("X-Gateway-Timestamp", timestamp)
                .header("X-Gateway-Nonce", nonce)
                .header("X-Gateway-Signature", signature)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}
