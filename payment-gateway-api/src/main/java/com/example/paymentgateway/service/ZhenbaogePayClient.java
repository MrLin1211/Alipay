package com.example.paymentgateway.service;

import com.example.paymentgateway.util.JsonUtils;
import com.example.paymentgateway.util.ZhenbaogeSignUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ZhenbaogePayClient {

    private final RestClient.Builder builder;

    public ZhenbaogePayClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    public JsonNode createPay(Map<String, Object> config, Map<String, Object> payload) {
        return post(config, "/api/pay/operPay", payload);
    }

    public JsonNode queryTrade(Map<String, Object> config, Map<String, Object> payload) {
        return post(config, "/api/pay/tradeQuery", payload);
    }

    public JsonNode refund(Map<String, Object> config, Map<String, Object> payload) {
        return post(config, "/api/pay/tradeRefund", payload);
    }

    private JsonNode post(Map<String, Object> config, String path, Map<String, Object> payload) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String md5Key = string(config.get("md5_key"));
        String aesKey = string(config.get("aes_key"));
        String visitAuth = ZhenbaogeSignUtils.visitAuth(md5Key, aesKey, timestamp);

        Map<String, Object> signedPayload = new LinkedHashMap<>(payload);
        signedPayload.put("sign", ZhenbaogeSignUtils.sign(signedPayload, visitAuth, aesKey));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        signedPayload.forEach((key, value) -> {
            if (value != null) {
                form.add(key, String.valueOf(value));
            }
        });

        String response = builder.baseUrl(string(config.get("host"))).build()
                .post()
                .uri(path)
                .header("timeStamp", timestamp)
                .header("visitAuth", visitAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(String.class);
        return JsonUtils.readTree(response == null ? "{}" : response);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
