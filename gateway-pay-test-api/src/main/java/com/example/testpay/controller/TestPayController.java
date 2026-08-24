package com.example.testpay.controller;

import com.example.testpay.config.TestPayProperties;
import com.example.testpay.domain.CreateTestPayRequest;
import com.example.testpay.domain.QueryTestPayRequest;
import com.example.testpay.service.GatewayOpenApiClient;
import com.example.testpay.service.NotifyStore;
import com.example.testpay.util.HmacUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/testpay")
public class TestPayController {

    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final GatewayOpenApiClient gatewayClient;
    private final TestPayProperties properties;
    private final NotifyStore notifyStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public TestPayController(GatewayOpenApiClient gatewayClient, TestPayProperties properties, NotifyStore notifyStore) {
        this.gatewayClient = gatewayClient;
        this.properties = properties;
        this.notifyStore = notifyStore;
    }

    @PostMapping("/orders")
    public JsonNode createOrder(@Valid @RequestBody CreateTestPayRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String merchantOrderNo = generateMerchantOrderNo();
        payload.put("merchantOrderNo", merchantOrderNo);
        payload.put("subject", request.getSubject().trim());
        payload.put("totalAmount", request.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        int typeIndex = request.getTypeIndex() == null ? 2 : request.getTypeIndex();
        payload.put("typeIndex", typeIndex);
        payload.put("goodsType", 1);
        payload.put("payMethodType", firstNonBlank(request.getPayMethodType(), "ALIPAY_CN"));
        String returnUrl = firstNonBlank(request.getReturnUrl(), properties.getReturnUrl());
        if (StringUtils.hasText(returnUrl)) {
            payload.put("returnUrl", appendMerchantOrderNo(returnUrl, merchantOrderNo));
        }
        String quitUrl = firstNonBlank(request.getQuitUrl(), typeIndex == 1 ? returnUrl : "");
        if (StringUtils.hasText(quitUrl)) {
            payload.put("quitUrl", appendMerchantOrderNo(quitUrl, merchantOrderNo));
        }
        payload.put("businessNotifyUrl", properties.getBusinessNotifyUrl());
        payload.put("attachInfo", firstNonBlank(request.getAttachInfo(), "gateway-pay-test"));
        return gatewayClient.createOrder(payload);
    }

    @PostMapping("/orders/query")
    public JsonNode queryOrder(@RequestBody QueryTestPayRequest request) {
        if (!StringUtils.hasText(request.getGatewayOrderNo()) && !StringUtils.hasText(request.getMerchantOrderNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "网关订单号和业务订单号至少填写一个");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (StringUtils.hasText(request.getGatewayOrderNo())) {
            payload.put("gatewayOrderNo", request.getGatewayOrderNo().trim());
        }
        if (StringUtils.hasText(request.getMerchantOrderNo())) {
            payload.put("merchantOrderNo", request.getMerchantOrderNo().trim());
        }
        return gatewayClient.queryOrder(payload);
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String receiveNotify(
            @RequestBody String body,
            @RequestHeader(value = "X-Gateway-App-Id", required = false) String appId,
            @RequestHeader(value = "X-Gateway-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Gateway-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Gateway-Signature", required = false) String signature
    ) {
        String signText = "POST\n/api/testpay/notify\n" + timestamp + "\n" + nonce + "\n" + body;
        boolean verified = properties.getAppId().equals(appId)
                && HmacUtils.matches(HmacUtils.sign(properties.getAppSecret(), signText), signature);
        notifyStore.add(body, verified);
        if (!verified) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "业务通知验签失败");
        }
        return "success";
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> notifications() {
        return notifyStore.list();
    }

    private String generateMerchantOrderNo() {
        return "TEST" + LocalDateTime.now().format(ORDER_TIME) + secureRandom.nextInt(1000, 10000);
    }

    static String appendMerchantOrderNo(String url, String merchantOrderNo) {
        return UriComponentsBuilder.fromUriString(url)
                .replaceQueryParam("merchantOrderNo", merchantOrderNo)
                .build()
                .encode()
                .toUriString();
    }

    private static String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
