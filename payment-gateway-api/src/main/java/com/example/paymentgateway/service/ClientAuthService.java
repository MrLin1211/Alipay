package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.GatewayJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ClientAuthService {

    private static final int SESSION_HOURS = 12;

    private final GatewayJdbcRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientAuthService(GatewayJdbcRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> user = repository.findOne(
                """
                        SELECT u.id, u.client_id, u.username, u.password_hash, u.display_name, u.role, u.enabled,
                               c.app_id, c.client_name, c.status AS client_status,
                               a.id AS app_row_id, a.app_name, a.enabled AS app_enabled
                        FROM gateway_client_user u
                        JOIN gateway_client c ON c.id = u.client_id
                        JOIN gateway_app a ON a.app_id = c.app_id
                        WHERE u.username = ?
                        """,
                username
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!isTrue(user.get("enabled"))
                || !isTrue(user.get("app_enabled"))
                || !"ACTIVE".equals(String.valueOf(user.get("client_status")))
                || !passwordEncoder.matches(password, String.valueOf(user.get("password_hash")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        repository.update("DELETE FROM gateway_client_session WHERE expires_at < NOW()");
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        repository.update(
                """
                        INSERT INTO gateway_client_session (
                            client_user_id, token, expires_at, created_at, updated_at
                        ) VALUES (?, ?, ?, NOW(), NOW())
                        """,
                user.get("id"),
                token,
                expiresAt
        );
        repository.update("UPDATE gateway_client_user SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?", user.get("id"));
        return Map.of("token", token, "expiresAt", expiresAt, "user", clientView(user));
    }

    public Map<String, Object> requireClient(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Map<String, Object> user = repository.findOne(
                """
                        SELECT u.id, u.client_id, u.username, u.display_name, u.role, u.enabled,
                               s.expires_at, c.app_id, c.client_name, c.status AS client_status,
                               a.id AS app_row_id, a.app_name, a.enabled AS app_enabled,
                               a.notify_url, a.ip_whitelist
                        FROM gateway_client_session s
                        JOIN gateway_client_user u ON u.id = s.client_user_id
                        JOIN gateway_client c ON c.id = u.client_id
                        JOIN gateway_app a ON a.app_id = c.app_id
                        WHERE s.token = ? AND s.expires_at > NOW()
                        """,
                token
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        if (!isTrue(user.get("enabled"))
                || !isTrue(user.get("app_enabled"))
                || !"ACTIVE".equals(String.valueOf(user.get("client_status")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
        }
        return user;
    }

    public Map<String, Object> profile(Map<String, Object> client) {
        Map<String, Object> profile = new LinkedHashMap<>(clientView(client));
        profile.put("app", appView(requireApp(client)));
        return profile;
    }

    public Map<String, Object> requireApp(Map<String, Object> client) {
        return repository.findOne(
                "SELECT * FROM gateway_app WHERE app_id = ? AND enabled = true",
                client.get("app_id")
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "接入应用不存在或已停用"));
    }

    public Map<String, Object> updateApp(Map<String, Object> client, Map<String, Object> payload) {
        repository.update(
                """
                        UPDATE gateway_app
                        SET notify_url = ?, ip_whitelist = ?, updated_at = NOW()
                        WHERE app_id = ?
                        """,
                value(payload, "notifyUrl"),
                value(payload, "ipWhitelist"),
                client.get("app_id")
        );
        return appView(requireApp(client));
    }

    public Map<String, Object> resetAppSecret(Map<String, Object> client) {
        String appSecret = generateAppSecret();
        repository.update(
                "UPDATE gateway_app SET app_secret = ?, updated_at = NOW() WHERE app_id = ?",
                appSecret,
                client.get("app_id")
        );
        return Map.of("appId", client.get("app_id"), "appSecret", appSecret);
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            repository.update("DELETE FROM gateway_client_session WHERE token = ?", token);
        }
    }

    private Map<String, Object> clientView(Map<String, Object> user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.get("id"));
        view.put("username", user.get("username"));
        view.put("displayName", user.get("display_name"));
        view.put("role", user.get("role"));
        view.put("clientName", user.get("client_name"));
        view.put("appId", user.get("app_id"));
        return view;
    }

    private Map<String, Object> appView(Map<String, Object> app) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("appId", app.get("app_id"));
        view.put("appName", app.get("app_name"));
        view.put("enabled", app.get("enabled"));
        view.put("notifyUrl", app.get("notify_url"));
        view.put("ipWhitelist", app.get("ip_whitelist"));
        view.put("createdAt", app.get("created_at"));
        view.put("updatedAt", app.get("updated_at"));
        return view;
    }

    private String generateToken() {
        return generateSecretBytes(48);
    }

    private String generateAppSecret() {
        return generateSecretBytes(32);
    }

    private String generateSecretBytes(int byteSize) {
        byte[] bytes = new byte[byteSize];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }
}
