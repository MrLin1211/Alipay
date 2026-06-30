package com.example.paymentgateway.controller;

import com.example.paymentgateway.service.AlipayGatewayService;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AlipayNotifyController {

    private final AlipayGatewayService alipayGatewayService;

    public AlipayNotifyController(AlipayGatewayService alipayGatewayService) {
        this.alipayGatewayService = alipayGatewayService;
    }

    @PostMapping(
            value = {"/api/gateway/alipay/notify", "/api/gateway/notify/alipay"},
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String notify(@RequestParam MultiValueMap<String, String> form) {
        boolean success = alipayGatewayService.handleNotify(firstValueMap(form));
        return success ? "success" : "fail";
    }

    private static Map<String, String> firstValueMap(MultiValueMap<String, String> form) {
        Map<String, String> params = new LinkedHashMap<>();
        form.forEach((key, values) -> params.put(key, values == null || values.isEmpty() ? "" : values.get(0)));
        return params;
    }
}
