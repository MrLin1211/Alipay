package com.example.mallhome.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerAuthService {

    private static final int SESSION_HOURS = 24 * 7;

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerAuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> register(Map<String, Object> payload) {
        String phone = requiredPhone(payload, "phone");
        String password = required(payload, "password", "请输入登录密码");
        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少6位");
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mall_user WHERE phone = ?", Integer.class, phone);
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号已注册");
        }
        String displayName = value(payload, "displayName");
        if (!StringUtils.hasText(displayName)) {
            displayName = "用户" + phone.substring(7);
        }
        jdbcTemplate.update(
                """
                        INSERT INTO mall_user (
                            phone, password_hash, display_name, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, true, NOW(), NOW())
                        """,
                phone,
                passwordEncoder.encode(password),
                displayName
        );
        return login(Map.of("phone", phone, "password", password));
    }

    @Transactional
    public Map<String, Object> login(Map<String, Object> payload) {
        String phone = requiredPhone(payload, "phone");
        String password = required(payload, "password", "请输入登录密码");
        Map<String, Object> user = findOne(
                "SELECT id, phone, password_hash, display_name, enabled FROM mall_user WHERE phone = ?",
                phone
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        if (!Boolean.TRUE.equals(user.get("enabled")) || !passwordEncoder.matches(password, String.valueOf(user.get("password_hash")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        jdbcTemplate.update("DELETE FROM mall_user_session WHERE expires_at < NOW()");
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        jdbcTemplate.update(
                """
                        INSERT INTO mall_user_session (
                            user_id, token, expires_at, created_at, updated_at
                        ) VALUES (?, ?, ?, NOW(), NOW())
                        """,
                user.get("id"),
                token,
                expiresAt
        );
        jdbcTemplate.update("UPDATE mall_user SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?", user.get("id"));
        return Map.of("token", token, "expiresAt", expiresAt, "user", userView(user));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> requireUser(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Map<String, Object> user = findOne(
                """
                        SELECT u.id, u.phone, u.display_name, u.enabled, s.expires_at
                        FROM mall_user_session s
                        JOIN mall_user u ON u.id = s.user_id
                        WHERE s.token = ? AND s.expires_at > NOW()
                        """,
                token
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        if (!Boolean.TRUE.equals(user.get("enabled"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
        }
        return user;
    }

    @Transactional
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            jdbcTemplate.update("DELETE FROM mall_user_session WHERE token = ?", token);
        }
    }

    public Map<String, Object> userView(Map<String, Object> user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.get("id"));
        view.put("phone", user.get("phone"));
        view.put("displayName", user.get("display_name"));
        return view;
    }

    private Optional<Map<String, Object>> findOne(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private String requiredPhone(Map<String, Object> payload, String key) {
        String value = required(payload, key, "请输入11位手机号");
        if (!value.matches("^1\\d{10}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "登录账号必须是11位手机号");
        }
        return value;
    }

    private String required(Map<String, Object> payload, String key, String message) {
        String value = value(payload, key);
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
