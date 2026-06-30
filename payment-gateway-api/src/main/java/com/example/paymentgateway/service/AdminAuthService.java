package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.GatewayJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminAuthService {

    private final GatewayJdbcRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(GatewayJdbcRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> user = repository.findOne(
                "SELECT * FROM gateway_admin_user WHERE username = ? AND enabled = true",
                username
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        if (!passwordEncoder.matches(password, String.valueOf(user.get("password_hash")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        repository.update(
                """
                        INSERT INTO gateway_admin_session (
                            admin_user_id, token, expires_at, created_at, updated_at
                        ) VALUES (?, ?, ?, NOW(), NOW())
                        """,
                user.get("id"),
                token,
                LocalDateTime.now().plusHours(12)
        );
        user.remove("password_hash");
        return Map.of("token", token, "user", user);
    }

    public Map<String, Object> requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Map<String, Object> user = repository.findOne(
                """
                        SELECT u.id, u.username, u.display_name, u.role, u.enabled
                        FROM gateway_admin_session s
                        JOIN gateway_admin_user u ON u.id = s.admin_user_id
                        WHERE s.token = ? AND s.expires_at > NOW() AND u.enabled = true
                        """,
                token
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        return user;
    }
}
