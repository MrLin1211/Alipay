package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.AdminLoginRequest;
import com.example.paymentgateway.domain.ApiResponse;
import com.example.paymentgateway.domain.RefundRequest;
import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.service.AlipayGatewayService;
import com.example.paymentgateway.service.ClientAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway/client")
public class ClientPayAdminController {

    private final ClientAuthService authService;
    private final GatewayJdbcRepository repository;
    private final AlipayGatewayService gatewayService;

    public ClientPayAdminController(
            ClientAuthService authService,
            GatewayJdbcRepository repository,
            AlipayGatewayService gatewayService
    ) {
        this.authService = authService;
        this.repository = repository;
        this.gatewayService = gatewayService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(extractToken(request));
        return ApiResponse.success(null);
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        return ApiResponse.success(authService.profile(currentClient(request)));
    }

    @GetMapping("/app")
    public ApiResponse<Map<String, Object>> getApp(HttpServletRequest request) {
        return ApiResponse.success(authService.profile(currentClient(request)).get("app") instanceof Map<?, ?> app
                ? (Map<String, Object>) app
                : Map.of());
    }

    @PutMapping("/app")
    public ApiResponse<Map<String, Object>> updateApp(
            HttpServletRequest request,
            @RequestBody Map<String, Object> payload
    ) {
        return ApiResponse.success(authService.updateApp(currentClient(request), payload));
    }

    @PostMapping("/app/secret/reset")
    public ApiResponse<Map<String, Object>> resetAppSecret(HttpServletRequest request) {
        return ApiResponse.success(authService.resetAppSecret(currentClient(request)));
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> listOrders(
            HttpServletRequest request,
            @RequestParam(required = false) String gatewayOrderNo,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platformTradeNo,
            @RequestParam(required = false) String thirdTradeNo,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String appId = appId(request);
        Query query = buildQuery("""
                FROM gateway_pay_order
                WHERE app_id = ?
                """, appId);
        addLike(query, "gateway_order_no", gatewayOrderNo);
        addLike(query, "merchant_order_no", merchantOrderNo);
        addEquals(query, "status", status);
        addLike(query, "platform_trade_no", platformTradeNo);
        addLike(query, "alipay_trade_no", thirdTradeNo);
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT id, gateway_order_no, app_id, merchant_order_no, channel, product_code,
                               subject, total_amount, status, trade_status, platform_trade_no, alipay_trade_no,
                               request_payload, channel_response, notify_payload, business_notify_url,
                               business_notify_result, business_notified_at, paid_at, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        Long total = repository.jdbc().queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(page(content, safePage, safeSize, total));
    }

    @PostMapping("/refunds")
    public ApiResponse<Map<String, Object>> createRefund(
            HttpServletRequest request,
            @Valid @RequestBody RefundRequest refundRequest
    ) {
        return ApiResponse.success(gatewayService.refund(authService.requireApp(currentClient(request)), refundRequest));
    }

    @GetMapping("/refunds")
    public ApiResponse<Map<String, Object>> listRefunds(
            HttpServletRequest request,
            @RequestParam(required = false) String refundOrderNo,
            @RequestParam(required = false) String gatewayOrderNo,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Query query = buildQuery("""
                FROM gateway_refund_order
                WHERE app_id = ?
                """, appId(request));
        addLike(query, "refund_order_no", refundOrderNo);
        addLike(query, "gateway_order_no", gatewayOrderNo);
        addLike(query, "merchant_order_no", merchantOrderNo);
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
        return ApiResponse.success(page(content, safePage, safeSize, total));
    }

    @GetMapping("/notifies")
    public ApiResponse<Map<String, Object>> listNotifies(
            HttpServletRequest request,
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
        Query query = buildQuery("""
                FROM gateway_notify_record n
                JOIN gateway_pay_order o ON o.gateway_order_no = n.gateway_order_no
                WHERE o.app_id = ?
                """, appId(request));
        addLike(query, "n.gateway_order_no", gatewayOrderNo);
        addLike(query, "n.merchant_order_no", merchantOrderNo);
        addLike(query, "n.platform_trade_no", platformTradeNo);
        addEquals(query, "n.trade_status", tradeStatus);
        if (verified != null) {
            query.sql += " AND n.verified = ?";
            query.args.add(verified);
        }
        addEquals(query, "n.result", result);
        addDateRange(query, "n.updated_at", updatedAtStart, updatedAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT n.id, n.gateway_order_no, n.merchant_order_no, n.platform_trade_no,
                               n.channel, n.channel_trade_no,
                               n.trade_status, n.notify_key, n.verified, n.result, n.failure_reason,
                               n.notify_payload, n.created_at, n.updated_at
                        """ + query.sql + " ORDER BY n.id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        Long total = repository.jdbc().queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(page(content, safePage, safeSize, total));
    }

    private Map<String, Object> currentClient(HttpServletRequest request) {
        return authService.requireClient(extractToken(request));
    }

    private String appId(HttpServletRequest request) {
        return String.valueOf(currentClient(request).get("app_id"));
    }

    private static String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    private Query buildQuery(String sql, Object... args) {
        Query query = new Query();
        query.sql = sql;
        query.args.addAll(List.of(args));
        return query;
    }

    private void addLike(Query query, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        query.sql += " AND " + column + " LIKE ?";
        query.args.add("%" + value.trim() + "%");
    }

    private void addEquals(Query query, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        query.sql += " AND " + column + " = ?";
        query.args.add(value.trim());
    }

    private void addDateRange(Query query, String column, String start, String end) {
        if (start != null && !start.isBlank()) {
            query.sql += " AND " + column + " >= ?";
            query.args.add(start);
        }
        if (end != null && !end.isBlank()) {
            query.sql += " AND " + column + " <= ?";
            query.args.add(end);
        }
    }

    private static Object[] append(List<Object> args, Object... values) {
        List<Object> result = new ArrayList<>(args);
        result.addAll(List.of(values));
        return result.toArray();
    }

    private static Map<String, Object> page(List<Map<String, Object>> content, int page, int size, Long total) {
        return Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", total == null ? 0 : total
        );
    }

    private static class Query {
        String sql;
        List<Object> args = new ArrayList<>();
    }
}
