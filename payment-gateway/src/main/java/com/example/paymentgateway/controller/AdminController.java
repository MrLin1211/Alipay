package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.AdminLoginRequest;
import com.example.paymentgateway.domain.ApiResponse;
import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.service.AdminAuthService;
import com.example.paymentgateway.service.AlipayGatewayService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway/admin")
public class AdminController {

    private final AdminAuthService authService;
    private final AlipayGatewayService alipayGatewayService;
    private final GatewayJdbcRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminController(
            AdminAuthService authService,
            AlipayGatewayService alipayGatewayService,
            GatewayJdbcRepository repository
    ) {
        this.authService = authService;
        this.alipayGatewayService = alipayGatewayService;
        this.repository = repository;
    }

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/alipay/config")
    public ApiResponse<Map<String, Object>> getAlipayConfig() {
        Map<String, Object> config = new LinkedHashMap<>(alipayGatewayService.getConfig());
        config.remove("app_private_key");
        config.remove("alipay_public_key");
        return ApiResponse.success(config);
    }

    @PutMapping("/alipay/config")
    public ApiResponse<Void> saveAlipayConfig(@RequestBody Map<String, Object> payload) {
        alipayGatewayService.saveConfig(payload);
        return ApiResponse.success(null);
    }

    @GetMapping("/apps")
    public ApiResponse<List<Map<String, Object>>> listApps(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Boolean enabled
    ) {
        PageQuery query = buildPageQuery("""
                FROM gateway_app
                WHERE 1 = 1
                """);
        addLike(query, "app_id", appId);
        addLike(query, "app_name", appName);
        if (enabled != null) {
            query.sql += " AND enabled = ?";
            query.args.add(enabled);
        }
        List<Map<String, Object>> apps = repository.findList(
                """
                        SELECT id, app_id, app_name, enabled, notify_url, ip_whitelist, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC",
                query.args.toArray()
        );
        return ApiResponse.success(apps);
    }

    @PostMapping("/apps")
    public ApiResponse<Map<String, Object>> createApp(@RequestBody Map<String, Object> payload) {
        String appId = generateUniqueAppId();
        String appSecret = generateAppSecret();
        repository.update(
                """
                        INSERT INTO gateway_app (
                            app_id, app_secret, app_name, enabled, notify_url, ip_whitelist, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                appId,
                appSecret,
                payload.get("appName"),
                Boolean.TRUE.equals(payload.get("enabled")),
                payload.get("notifyUrl"),
                payload.get("ipWhitelist")
        );
        return ApiResponse.success(Map.of(
                "appId", appId,
                "appSecret", appSecret,
                "appName", payload.getOrDefault("appName", "")
        ));
    }

    @PutMapping("/apps/{id}")
    public ApiResponse<Void> updateApp(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        int affected = repository.update(
                """
                        UPDATE gateway_app
                        SET app_name = ?, enabled = ?, notify_url = ?, ip_whitelist = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                payload.get("appName"),
                Boolean.TRUE.equals(payload.get("enabled")),
                payload.get("notifyUrl"),
                payload.get("ipWhitelist"),
                id
        );
        if (affected == 0) {
            throw new IllegalArgumentException("应用不存在");
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> listOrders(
            @RequestParam(required = false) String gatewayOrderNo,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alipayTradeNo,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM gateway_pay_order
                WHERE 1 = 1
                """);
        addLike(query, "gateway_order_no", gatewayOrderNo);
        addLike(query, "merchant_order_no", merchantOrderNo);
        addEquals(query, "app_id", appId);
        addEquals(query, "status", status);
        addLike(query, "alipay_trade_no", alipayTradeNo);
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT id, gateway_order_no, app_id, merchant_order_no, channel, product_code,
                               subject, total_amount, status, trade_status, alipay_trade_no,
                               request_payload, channel_response, notify_payload, paid_at, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        Long total = repository.jdbc().queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(Map.of(
                "content", content,
                "page", safePage,
                "size", safeSize,
                "totalElements", total == null ? 0 : total
        ));
    }

    @GetMapping("/notifies")
    public ApiResponse<Map<String, Object>> listNotifies(
            @RequestParam(required = false) String gatewayOrderNo,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String tradeStatus,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String updatedAtStart,
            @RequestParam(required = false) String updatedAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM gateway_notify_record
                WHERE 1 = 1
                """);
        addLike(query, "gateway_order_no", gatewayOrderNo);
        addLike(query, "merchant_order_no", merchantOrderNo);
        addEquals(query, "trade_status", tradeStatus);
        if (verified != null) {
            query.sql += " AND verified = ?";
            query.args.add(verified);
        }
        addEquals(query, "result", result);
        addDateRange(query, "updated_at", updatedAtStart, updatedAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT id, gateway_order_no, merchant_order_no, channel, channel_trade_no,
                               trade_status, notify_key, verified, result, failure_reason,
                               notify_payload, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        Long total = repository.jdbc().queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(Map.of(
                "content", content,
                "page", safePage,
                "size", safeSize,
                "totalElements", total == null ? 0 : total
        ));
    }

    private PageQuery buildPageQuery(String sql) {
        PageQuery query = new PageQuery();
        query.sql = sql;
        return query;
    }

    private void addEquals(PageQuery query, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        query.sql += " AND " + column + " = ?";
        query.args.add(value);
    }

    private void addLike(PageQuery query, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        query.sql += " AND " + column + " LIKE ?";
        query.args.add("%" + value.trim() + "%");
    }

    private void addDateRange(PageQuery query, String column, String start, String end) {
        if (start != null && !start.isBlank()) {
            query.sql += " AND " + column + " >= ?";
            query.args.add(start.replace("T", " "));
        }
        if (end != null && !end.isBlank()) {
            query.sql += " AND " + column + " <= ?";
            query.args.add(end.replace("T", " "));
        }
    }

    private Object[] append(List<Object> args, Object... values) {
        List<Object> all = new ArrayList<>(args);
        all.addAll(List.of(values));
        return all.toArray();
    }

    private String generateUniqueAppId() {
        for (int i = 0; i < 10; i++) {
            String appId = "gwapp_" + randomUrlToken(12).toLowerCase();
            if (repository.findOne("SELECT id FROM gateway_app WHERE app_id = ?", appId).isEmpty()) {
                return appId;
            }
        }
        throw new IllegalStateException("AppId生成失败，请重试");
    }

    private String generateAppSecret() {
        return "gwsec_" + randomUrlToken(32);
    }

    private String randomUrlToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static class PageQuery {
        private String sql;
        private final List<Object> args = new ArrayList<>();
    }
}
