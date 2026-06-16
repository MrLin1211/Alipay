package com.example.mallhome.controller.server;

import com.example.mallhome.domain.PayNotifyResult;
import com.example.mallhome.service.PayNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/server/gateway/pay")
public class GatewayPayNotifyController {

    private static final Logger log = LoggerFactory.getLogger(GatewayPayNotifyController.class);

    private final PayNotifyService payNotifyService;

    public GatewayPayNotifyController(PayNotifyService payNotifyService) {
        this.payNotifyService = payNotifyService;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String receiveGatewayNotify(
            @RequestHeader(value = "X-Gateway-App-Id", required = false) String appId,
            @RequestHeader(value = "X-Gateway-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Gateway-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Gateway-Signature", required = false) String signature,
            @RequestBody String body
    ) {
        PayNotifyResult result = payNotifyService.handleGatewayNotify(appId, timestamp, nonce, signature, body);
        if (!result.isSuccess()) {
            log.warn("gateway payment notify rejected: {}", result.getMessage());
            return "fail";
        }
        return "success";
    }
}
