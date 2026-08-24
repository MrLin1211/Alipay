package com.example.testpay.service;

import com.example.testpay.config.TestPayProperties;
import com.example.testpay.util.HmacUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GatewayOpenApiClient {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TestPayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GatewayOpenApiClient(TestPayProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.getGatewayBaseUrl()).build();
    }

    public JsonNode createOrder(Map<String, Object> payload) {
        return post("/api/gateway/pay/orders", payload);
    }

    public JsonNode queryOrder(Map<String, Object> payload) {
        return post("/api/gateway/pay/orders/query", payload);
    }

    private JsonNode post(String path, Map<String, Object> payload) {
        requireConfiguration();
        try {
            String body = objectMapper.writeValueAsString(new LinkedHashMap<>(payload));
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String nonce = UUID.randomUUID().toString();
            String signText = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
            String signature = HmacUtils.sign(properties.getAppSecret(), signText);
            String response = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Gateway-App-Id", properties.getAppId())
                    .header("X-Gateway-Timestamp", timestamp)
                    .header("X-Gateway-Nonce", nonce)
                    .header("X-Gateway-Signature", signature)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response == null ? "{}" : response);
        } catch (RuntimeException runtimeException) {
            throw runtimeException;
        } catch (Exception exception) {
            throw new IllegalStateException("调用支付网关失败", exception);
        }
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(properties.getGatewayBaseUrl())
                || !StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getAppSecret())) {
            throw new IllegalStateException("测试端网关配置不完整");
        }
    }
}
