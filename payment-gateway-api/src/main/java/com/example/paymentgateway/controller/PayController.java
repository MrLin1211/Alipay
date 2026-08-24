package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.ApiResponse;
import com.example.paymentgateway.domain.CreateWapPayRequest;
import com.example.paymentgateway.domain.CreateWapPayResponse;
import com.example.paymentgateway.domain.RefundRequest;
import com.example.paymentgateway.domain.TradeQueryRequest;
import com.example.paymentgateway.service.AlipayGatewayService;
import com.example.paymentgateway.service.AppAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/gateway/pay")
@Validated
public class PayController {

    private final AppAuthService appAuthService;
    private final AlipayGatewayService alipayGatewayService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PayController(AppAuthService appAuthService, AlipayGatewayService alipayGatewayService,
                         ObjectMapper objectMapper, Validator validator) {
        this.appAuthService = appAuthService;
        this.alipayGatewayService = alipayGatewayService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping("/alipay/wap")
    public ApiResponse<CreateWapPayResponse> createWapPay(HttpServletRequest request, @RequestBody String body) throws Exception {
        return createOrder(request, body);
    }

    @PostMapping({"/orders", "/create"})
    public ApiResponse<CreateWapPayResponse> createOrder(HttpServletRequest request, @RequestBody String body) throws Exception {
        Map<String, Object> app = appAuthService.requireApp(request, body);
        CreateWapPayRequest payRequest = validate(objectMapper.readValue(body, CreateWapPayRequest.class));
        if (payRequest.getClientIp() == null || payRequest.getClientIp().isBlank()) {
            payRequest.setClientIp(clientIp(request));
        }
        CreateWapPayResponse response = alipayGatewayService.createWapPay(app, payRequest);
        return ApiResponse.success(response);
    }

    @PostMapping("/orders/query")
    public ApiResponse<Map<String, Object>> queryOrder(HttpServletRequest request, @RequestBody String body) throws Exception {
        Map<String, Object> app = appAuthService.requireApp(request, body);
        TradeQueryRequest queryRequest = validate(objectMapper.readValue(body, TradeQueryRequest.class));
        return ApiResponse.success(alipayGatewayService.queryTrade(app, queryRequest));
    }

    @PostMapping("/refunds")
    public ApiResponse<Map<String, Object>> refund(HttpServletRequest request, @RequestBody String body) throws Exception {
        Map<String, Object> app = appAuthService.requireApp(request, body);
        RefundRequest refundRequest = validate(objectMapper.readValue(body, RefundRequest.class));
        return ApiResponse.success(alipayGatewayService.refund(app, refundRequest));
    }

    private <T> T validate(T value) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        violations.stream()
                .min(Comparator.comparing(item -> item.getPropertyPath().toString()))
                .ifPresent(item -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, item.getMessage());
                });
        return value;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
