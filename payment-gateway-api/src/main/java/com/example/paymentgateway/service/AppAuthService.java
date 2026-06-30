package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.util.HmacUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AppAuthService {

    private final GatewayJdbcRepository repository;
    public AppAuthService(GatewayJdbcRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> requireApp(HttpServletRequest request, String body) {
        String appId = request.getHeader("X-Gateway-App-Id");
        String timestamp = request.getHeader("X-Gateway-Timestamp");
        String nonce = request.getHeader("X-Gateway-Nonce");
        String signature = request.getHeader("X-Gateway-Signature");
        if (isBlank(appId) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "内部应用鉴权参数缺失");
        }

        Map<String, Object> app = repository.findOne(
                "SELECT * FROM gateway_app WHERE app_id = ? AND enabled = true",
                appId
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "内部应用不存在或已禁用"));

        String payload = request.getMethod() + "\n" + request.getRequestURI() + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String expected = HmacUtils.hmacSha256Base64(String.valueOf(app.get("app_secret")), payload);
        if (!expected.equals(signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "内部应用签名错误");
        }
        return app;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
