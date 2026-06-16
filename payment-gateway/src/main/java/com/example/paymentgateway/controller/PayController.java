package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.ApiResponse;
import com.example.paymentgateway.domain.CreateWapPayRequest;
import com.example.paymentgateway.domain.CreateWapPayResponse;
import com.example.paymentgateway.service.AlipayGatewayService;
import com.example.paymentgateway.service.AppAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/gateway/pay")
@Validated
public class PayController {

    private final AppAuthService appAuthService;
    private final AlipayGatewayService alipayGatewayService;
    private final ObjectMapper objectMapper;

    public PayController(AppAuthService appAuthService, AlipayGatewayService alipayGatewayService, ObjectMapper objectMapper) {
        this.appAuthService = appAuthService;
        this.alipayGatewayService = alipayGatewayService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/alipay/wap")
    public ApiResponse<CreateWapPayResponse> createWapPay(HttpServletRequest request, @RequestBody String body) throws Exception {
        Map<String, Object> app = appAuthService.requireApp(request, body);
        CreateWapPayRequest payRequest = objectMapper.readValue(body, CreateWapPayRequest.class);
        CreateWapPayResponse response = alipayGatewayService.createWapPay(app, payRequest);
        return ApiResponse.success(response);
    }
}
