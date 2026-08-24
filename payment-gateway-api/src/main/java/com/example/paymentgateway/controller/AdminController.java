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

    @GetMapping({"/alipay/config", "/channel/config"})
    public ApiResponse<Map<String, Object>> getAlipayConfig() {
        Map<String, Object> config = new LinkedHashMap<>(alipayGatewayService.getConfig());
        config.remove("md5_key");
        config.remove("aes_key");
        return ApiResponse.success(config);
    }

    @PutMapping({"/alipay/config", "/channel/config"})
    public ApiResponse<Void> saveAlipayConfig(@RequestBody Map<String, Object> payload) {
        alipayGatewayService.saveConfig(payload);
        return ApiResponse.success(null);
    }

    @GetMapping("/apps")
    public ApiResponse<List<Map<String, Object>>> listApps(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd
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
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);
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
        String appName = value(payload, "appName");
        if (appName == null || appName.isBlank()) {
            appName = "未命名应用";
        }
        repository.update(
                """
                        INSERT INTO gateway_app (
                            app_id, app_secret, app_name, enabled, notify_url, ip_whitelist, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                appId,
                appSecret,
                appName,
                Boolean.TRUE.equals(payload.get("enabled")),
                payload.get("notifyUrl"),
                payload.get("ipWhitelist")
        );
        String clientPassword = generateClientPassword();
        repository.update(
                """
                        INSERT INTO gateway_client (
                            app_id, client_name, contact_name, contact_phone, status, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
                        """,
                appId,
                appName,
                "",
                ""
        );
        Long clientId = repository.jdbc().queryForObject("SELECT id FROM gateway_client WHERE app_id = ?", Long.class, appId);
        repository.update(
                """
                        INSERT INTO gateway_client_user (
                            client_id, username, password_hash, display_name, role, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, 'OWNER', true, NOW(), NOW())
                        """,
                clientId,
                appId,
                passwordEncoder.encode(clientPassword),
                appName
        );
        return ApiResponse.success(Map.of(
                "appId", appId,
                "appSecret", appSecret,
                "appName", appName,
                "clientUsername", appId,
                "clientPassword", clientPassword
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
        repository.findOne("SELECT app_id, app_name, enabled FROM gateway_app WHERE id = ?", id)
                .ifPresent(row -> repository.update(
                        """
                                UPDATE gateway_client
                                SET client_name = ?, status = ?, updated_at = NOW()
                                WHERE app_id = ?
                                """,
                        row.get("app_name"),
                        Boolean.TRUE.equals(bool(row.get("enabled"))) ? "ACTIVE" : "DISABLED",
                        row.get("app_id")
                ));
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
                FROM gateway_pay_order o
                JOIN gateway_app a ON a.app_id = o.app_id
                WHERE 1 = 1
                """);
        addLike(query, "o.gateway_order_no", gatewayOrderNo);
        addLike(query, "o.merchant_order_no", merchantOrderNo);
        addEquals(query, "o.app_id", appId);
        addEquals(query, "o.status", status);
        addLike(query, "o.alipay_trade_no", alipayTradeNo);
        addDateRange(query, "o.created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT o.id, o.gateway_order_no, o.app_id, a.app_name, o.merchant_order_no,
                               o.channel, o.product_code, o.subject, o.total_amount, o.status, o.trade_status,
                               o.platform_trade_no, o.alipay_trade_no, o.request_payload, o.channel_response,
                               o.notify_payload, o.paid_at, o.created_at, o.updated_at
                        """ + query.sql + " ORDER BY o.id DESC LIMIT ? OFFSET ?",
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

    @GetMapping("/refunds")
    public ApiResponse<Map<String, Object>> listRefunds(
            @RequestParam(required = false) String refundOrderNo,
            @RequestParam(required = false) String gatewayOrderNo,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String platformTradeNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM gateway_refund_order
                WHERE 1 = 1
                """);
        addLike(query, "refund_order_no", refundOrderNo);
        addLike(query, "gateway_order_no", gatewayOrderNo);
        addLike(query, "merchant_order_no", merchantOrderNo);
        addEquals(query, "app_id", appId);
        addLike(query, "platform_trade_no", platformTradeNo);
        addEquals(query, "status", status);
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT id, refund_order_no, gateway_order_no, app_id, merchant_order_no, platform_trade_no,
                               refund_amount, refund_reason, status, trade_status, request_payload,
                               platform_response, notify_payload, created_at, updated_at
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
            @RequestParam(required = false) String platformTradeNo,
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
        addLike(query, "platform_trade_no", platformTradeNo);
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
                        SELECT id, gateway_order_no, merchant_order_no, platform_trade_no, channel, channel_trade_no,
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

    private String generateClientPassword() {
        return "mp_" + randomUrlToken(18);
    }


    private String randomUrlToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    private String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Boolean bool(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static class PageQuery {
        private String sql;
        private final List<Object> args = new ArrayList<>();
    }
}
