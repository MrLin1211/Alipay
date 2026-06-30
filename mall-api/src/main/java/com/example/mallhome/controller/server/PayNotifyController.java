package com.example.mallhome.controller.server;

import com.example.mallhome.domain.PayNotifyResult;
import com.example.mallhome.service.PayNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class PayNotifyController {

    private static final Logger log = LoggerFactory.getLogger(PayNotifyController.class);

    private final PayNotifyService payNotifyService;

    public PayNotifyController(PayNotifyService payNotifyService) {
        this.payNotifyService = payNotifyService;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String receiveNotify(
            @RequestHeader(value = "timeStamp", required = false) String timeStamp,
            @RequestHeader(value = "visitAuth", required = false) String visitAuth,
            @RequestParam MultiValueMap<String, String> form
    ) {
        PayNotifyResult result = payNotifyService.handleNotify(timeStamp, visitAuth, firstValueMap(form));
        if (!result.isSuccess()) {
            log.warn("payment notify rejected: {}", result.getMessage());
            return "fail";
        }
        return "success";
    }

    private static Map<String, String> firstValueMap(MultiValueMap<String, String> form) {
        Map<String, String> params = new LinkedHashMap<>();
        form.forEach((key, values) -> params.put(key, values == null || values.isEmpty() ? "" : values.get(0)));
        return params;
    }
}
