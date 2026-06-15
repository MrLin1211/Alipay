package com.example.mallhome.controller.frontend;

import com.example.mallhome.domain.ClientIpResponse;
import com.example.mallhome.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ClientIpController {

    @GetMapping("/client-ip")
    public ClientIpResponse getClientIp(HttpServletRequest request) {
        return new ClientIpResponse(ClientIpUtils.resolveClientIp(request));
    }
}
