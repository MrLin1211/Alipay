package com.example.mallhome.client;

import com.example.mallhome.config.MallhomePayProperties;
import com.example.mallhome.util.FormUtils;
import com.example.mallhome.util.MallhomeSignUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Component
public class MallhomePayClient {

    private final MallhomePayProperties properties;
    private final RestClient restClient;

    public MallhomePayClient(MallhomePayProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.getHost()).build();
    }

    public String operPay(Map<String, String> paramsWithoutSign) {
        return postForm("/api/pay/operPay", paramsWithoutSign);
    }

    public String tradeRefund(Map<String, String> paramsWithoutSign) {
        return postForm("/api/pay/tradeRefund", paramsWithoutSign);
    }

    private String postForm(String path, Map<String, String> paramsWithoutSign) {
        // 平台接口要求秒级时间戳，并用 timeStamp + 密钥生成 visitAuth。
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String visitAuth = MallhomeSignUtils.buildVisitAuth(
                properties.getMd5Key(),
                properties.getAesKey(),
                timeStamp
        );

        // sign 必须放在表单 Body 中；参与签名的参数不包含 sign 自身。
        Map<String, String> signedParams = new TreeMap<>(paramsWithoutSign);
        signedParams.put("sign", MallhomeSignUtils.buildSign(signedParams, visitAuth, properties.getAesKey()));

        // 平台文档要求 application/x-www-form-urlencoded，不能用 JSON 直传。
        return restClient.post()
                .uri(path)
                .header("timeStamp", timeStamp)
                .header("visitAuth", visitAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(FormUtils.toFormBody(signedParams))
                .retrieve()
                .body(String.class);
    }
}
