package com.example.paymentgateway.controller;

import com.example.paymentgateway.domain.AdminLoginRequest;
import com.example.paymentgateway.domain.ApiResponse;
import com.example.paymentgateway.repository.GatewayJdbcRepository;
import com.example.paymentgateway.service.AdminAuthService;
import com.example.paymentgateway.service.AlipayGatewayService;
import com.example.paymentgateway.service.ProductOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.security.SecureRandom;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/gateway/admin")
public class AdminController {

    private final AdminAuthService authService;
    private final AlipayGatewayService alipayGatewayService;
    private final GatewayJdbcRepository repository;
    private final ProductOrderService productOrderService;
    private final JdbcTemplate mallhomeJdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(
            AdminAuthService authService,
            AlipayGatewayService alipayGatewayService,
            GatewayJdbcRepository repository,
            ProductOrderService productOrderService,
            @Qualifier("mallhomeJdbcTemplate") JdbcTemplate mallhomeJdbcTemplate
    ) {
        this.authService = authService;
        this.alipayGatewayService = alipayGatewayService;
        this.repository = repository;
        this.productOrderService = productOrderService;
        this.mallhomeJdbcTemplate = mallhomeJdbcTemplate;
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

    @GetMapping("/merchants")
    public ApiResponse<Map<String, Object>> listMerchants(
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM merchant m
                LEFT JOIN merchant_user u ON u.merchant_id = m.id AND u.role = 'OWNER'
                LEFT JOIN gateway_app a ON a.app_id = m.merchant_no
                WHERE 1 = 1
                """);
        addLike(query, "m.merchant_no", merchantNo);
        addLike(query, "m.merchant_name", merchantName);
        addLike(query, "u.username", username);
        addEquals(query, "m.status", status);
        addDateRange(query, "m.created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT m.id, m.merchant_no, m.merchant_name, m.contact_name, m.contact_phone,
                               m.status, m.created_at, m.updated_at,
                               u.id AS user_id, u.username, u.display_name, u.enabled AS user_enabled,
                               u.last_login_at,
                               a.id AS app_id_row, a.app_id, a.app_name, a.enabled AS app_enabled,
                               a.notify_url, a.ip_whitelist, a.updated_at AS app_updated_at
                        """ + query.sql + " ORDER BY m.id DESC LIMIT ? OFFSET ?",
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

    @PutMapping("/merchants/{id}")
    public ApiResponse<Void> updateMerchant(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String merchantName = requiredString(payload, "merchantName", "请输入商家名称");
        String status = value(payload, "status");
        if (!List.of("ACTIVE", "DISABLED").contains(status)) {
            throw new IllegalArgumentException("商家状态不正确");
        }
        Boolean userEnabled = bool(payload.get("userEnabled"));
        int affected = repository.update(
                """
                        UPDATE merchant
                        SET merchant_name = ?, contact_name = ?, contact_phone = ?, status = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                merchantName,
                value(payload, "contactName"),
                value(payload, "contactPhone"),
                status,
                id
        );
        if (affected == 0) {
            throw new IllegalArgumentException("商家不存在");
        }
        if (userEnabled != null) {
            repository.update(
                    "UPDATE merchant_user SET enabled = ?, updated_at = NOW() WHERE merchant_id = ? AND role = 'OWNER'",
                    userEnabled,
                    id
            );
        }
        repository.findOne("SELECT merchant_no FROM merchant WHERE id = ?", id)
                .ifPresent(row -> repository.update(
                        "UPDATE gateway_app SET app_name = ?, enabled = ?, updated_at = NOW() WHERE app_id = ?",
                        merchantName,
                        "ACTIVE".equals(status),
                        row.get("merchant_no")
                ));
        return ApiResponse.success(null);
    }

    @PutMapping("/merchants/{id}/app")
    public ApiResponse<Void> updateMerchantApp(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> merchant = repository.findOne("SELECT merchant_no, merchant_name FROM merchant WHERE id = ?", id)
                .orElseThrow(() -> new IllegalArgumentException("商家不存在"));
        String appId = String.valueOf(merchant.get("merchant_no"));
        String appName = value(payload, "appName");
        if (appName == null || appName.isBlank()) {
            appName = String.valueOf(merchant.get("merchant_name"));
        }
        Boolean enabled = bool(payload.get("enabled"));
        if (enabled == null) {
            enabled = true;
        }
        if (repository.findOne("SELECT id FROM gateway_app WHERE app_id = ?", appId).isEmpty()) {
            repository.update(
                    """
                            INSERT INTO gateway_app (
                                app_id, app_secret, app_name, enabled, notify_url, ip_whitelist, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                            """,
                    appId,
                    generateAppSecret(),
                    appName,
                    enabled,
                    value(payload, "notifyUrl"),
                    value(payload, "ipWhitelist")
            );
            return ApiResponse.success(null);
        }
        repository.update(
                """
                        UPDATE gateway_app
                        SET app_name = ?, enabled = ?, notify_url = ?, ip_whitelist = ?, updated_at = NOW()
                        WHERE app_id = ?
                        """,
                appName,
                enabled,
                value(payload, "notifyUrl"),
                value(payload, "ipWhitelist"),
                appId
        );
        return ApiResponse.success(null);
    }

    @GetMapping("/mall-users")
    public ApiResponse<Map<String, Object>> listMallUsers(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM mall_user
                WHERE 1 = 1
                """);
        addLike(query, "phone", phone);
        addLike(query, "display_name", displayName);
        if (enabled != null) {
            query.sql += " AND enabled = ?";
            query.args.add(enabled);
        }
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = mallhomeJdbcTemplate.queryForList(
                """
                        SELECT id, phone, display_name, enabled, last_login_at, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        Long total = mallhomeJdbcTemplate.queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(Map.of(
                "content", content,
                "page", safePage,
                "size", safeSize,
                "totalElements", total == null ? 0 : total
        ));
    }

    @PutMapping("/mall-users/{id}")
    public ApiResponse<Void> updateMallUser(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String displayName = requiredString(payload, "displayName", "请输入用户昵称");
        Boolean enabled = bool(payload.get("enabled"));
        int affected = mallhomeJdbcTemplate.update(
                """
                        UPDATE mall_user
                        SET display_name = ?, enabled = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                displayName,
                enabled == null || enabled,
                id
        );
        if (affected == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/addresses")
    public ApiResponse<Map<String, Object>> listAddresses(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM address a
                JOIN mall_user u ON u.id = a.user_id
                WHERE 1 = 1
                """);
        addLike(query, "u.phone", phone);
        addLike(query, "a.receiver_name", receiverName);
        if (keyword != null && !keyword.isBlank()) {
            query.sql += """
                     AND (
                        a.receiver_name LIKE ? OR a.phone LIKE ? OR a.province LIKE ?
                        OR a.city LIKE ? OR a.district LIKE ? OR a.detail_address LIKE ?
                     )
                    """;
            String like = "%" + keyword.trim() + "%";
            query.args.addAll(List.of(like, like, like, like, like, like));
        }
        addDateRange(query, "a.created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = mallhomeJdbcTemplate.queryForList(
                """
                        SELECT a.id, a.user_id AS userId, u.phone AS userPhone, u.display_name AS userDisplayName,
                               a.receiver_name AS receiverName, a.phone, a.province, a.city, a.district,
                               a.detail_address AS detailAddress, a.is_default AS isDefault,
                               a.created_at AS createdAt, a.updated_at AS updatedAt
                        """ + query.sql + " ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        Long total = mallhomeJdbcTemplate.queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(Map.of(
                "content", content,
                "page", safePage,
                "size", safeSize,
                "totalElements", total == null ? 0 : total
        ));
    }

    @PutMapping("/addresses/{id}")
    public ApiResponse<Void> updateAddress(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        int affected = mallhomeJdbcTemplate.update(
                """
                        UPDATE address
                        SET receiver_name = ?, phone = ?, province = ?, city = ?, district = ?,
                            detail_address = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                requiredString(payload, "receiverName", "请输入收货人"),
                requiredString(payload, "phone", "请输入收货电话"),
                requiredString(payload, "province", "请输入省份"),
                requiredString(payload, "city", "请输入城市"),
                requiredString(payload, "district", "请输入区县"),
                requiredString(payload, "detailAddress", "请输入详细地址"),
                id
        );
        if (affected == 0) {
            throw new IllegalArgumentException("地址不存在");
        }
        return ApiResponse.success(null);
    }

    @PutMapping("/addresses/{id}/default")
    public ApiResponse<Void> setDefaultAddress(@PathVariable Long id) {
        Map<String, Object> address = findAddress(id);
        Long userId = ((Number) address.get("user_id")).longValue();
        mallhomeJdbcTemplate.update("UPDATE address SET is_default = false, updated_at = NOW() WHERE user_id = ?", userId);
        mallhomeJdbcTemplate.update("UPDATE address SET is_default = true, updated_at = NOW() WHERE id = ?", id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        int affected = mallhomeJdbcTemplate.update("DELETE FROM address WHERE id = ?", id);
        if (affected == 0) {
            throw new IllegalArgumentException("地址不存在");
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> listCategories(
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd
    ) {
        PageQuery query = buildPageQuery("""
                FROM product_category
                WHERE 1 = 1
                """);
        addLike(query, "category_code", categoryCode);
        addLike(query, "category_name", categoryName);
        if (enabled != null) {
            query.sql += " AND enabled = ?";
            query.args.add(enabled);
        }
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);
        List<Map<String, Object>> categories = repository.findList(
                """
                        SELECT id, category_code, category_name, sort_order, enabled, created_at, updated_at
                        """ + query.sql + " ORDER BY sort_order ASC, id ASC",
                query.args.toArray()
        );
        return ApiResponse.success(categories);
    }

    @PostMapping("/categories")
    public ApiResponse<Map<String, Object>> createCategory(@RequestBody Map<String, Object> payload) {
        String categoryCode = generateUniqueCategoryCode();
        String categoryName = requiredString(payload, "categoryName", "请输入分类名称");
        repository.update(
                """
                        INSERT INTO product_category (
                            category_code, category_name, sort_order, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, NOW(), NOW())
                        """,
                categoryCode,
                categoryName,
                nonNegativeInt(payload.get("sortOrder"), "排序值不能小于0"),
                Boolean.TRUE.equals(payload.get("enabled"))
        );
        return ApiResponse.success(Map.of("categoryCode", categoryCode, "categoryName", categoryName));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String categoryName = requiredString(payload, "categoryName", "请输入分类名称");
        Boolean enabled = bool(payload.get("enabled"));
        int affected = repository.update(
                """
                        UPDATE product_category
                        SET category_name = ?, sort_order = ?, enabled = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                categoryName,
                nonNegativeInt(payload.get("sortOrder"), "排序值不能小于0"),
                enabled == null || enabled,
                id
        );
        if (affected == 0) {
            throw new IllegalArgumentException("分类不存在");
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> listProducts(
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = buildPageQuery("""
                FROM merchant_product
                WHERE 1 = 1
                """);
        addEquals(query, "merchant_no", merchantNo);
        addLike(query, "product_code", productCode);
        addLike(query, "product_name", productName);
        addLike(query, "category", category);
        addEquals(query, "status", status);
        addDateRange(query, "created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> content = repository.findList(
                """
                        SELECT id, merchant_no, product_code, product_name, category, price, stock, status,
                               cover_image, product_images, description, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                append(query.args, safeSize, safePage * safeSize)
        );
        content.forEach(this::attachSkus);
        Long total = repository.jdbc().queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return ApiResponse.success(Map.of(
                "content", content,
                "page", safePage,
                "size", safeSize,
                "totalElements", total == null ? 0 : total
        ));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<Map<String, Object>> getProduct(@PathVariable Long id) {
        Map<String, Object> product = repository.findOne(
                """
                        SELECT id, merchant_no, product_code, product_name, category, price, stock, status,
                               cover_image, product_images, description, created_at, updated_at
                        FROM merchant_product
                        WHERE id = ?
                        """,
                id
        ).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        attachSkus(product);
        return ApiResponse.success(product);
    }

    @PostMapping("/products")
    public ApiResponse<Map<String, Object>> createProduct(@RequestBody Map<String, Object> payload) {
        String merchantNo = requiredString(payload, "merchantNo", "请选择所属商家");
        ensureAppExists(merchantNo);
        String productCode = generateUniqueProductCode(merchantNo);
        String productName = requiredString(payload, "productName", "请输入商品名称");
        String category = requiredCategory(payload);
        List<Map<String, Object>> skus = skuPayloads(payload);
        BigDecimal price = skuPrice(skus);
        int stock = skuStock(skus);
        String status = productStatus(payload.get("status"));
        List<String> imageUrls = imageUrls(payload);

        repository.update(
                """
                        INSERT INTO merchant_product (
                            merchant_no, product_code, product_name, category, price, stock, status,
                            cover_image, product_images, description, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                merchantNo,
                productCode,
                productName,
                category,
                price,
                stock,
                status,
                firstImage(imageUrls),
                toJson(imageUrls),
                value(payload, "description")
        );
        Long productId = repository.findOne("SELECT id FROM merchant_product WHERE merchant_no = ? AND product_code = ?", merchantNo, productCode)
                .map(row -> Long.valueOf(String.valueOf(row.get("id"))))
                .orElseThrow(() -> new IllegalStateException("商品创建失败"));
        saveSkus(productId, productCode, skus);
        return ApiResponse.success(Map.of(
                "merchantNo", merchantNo,
                "productCode", productCode,
                "productName", productName
        ));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<Void> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> existingProduct = repository.findOne("SELECT merchant_no, product_code FROM merchant_product WHERE id = ?", id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        String productCode = String.valueOf(existingProduct.get("product_code"));
        String productName = requiredString(payload, "productName", "请输入商品名称");
        String category = requiredCategory(payload);
        List<Map<String, Object>> skus = skuPayloads(payload);
        BigDecimal price = skuPrice(skus);
        int stock = skuStock(skus);
        String status = productStatus(payload.get("status"));
        List<String> imageUrls = imageUrls(payload);

        int affected = repository.update(
                """
                        UPDATE merchant_product
                        SET product_code = ?, product_name = ?, category = ?, price = ?,
                            stock = ?, status = ?, cover_image = ?, product_images = ?, description = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                productCode,
                productName,
                category,
                price,
                stock,
                status,
                firstImage(imageUrls),
                toJson(imageUrls),
                value(payload, "description"),
                id
        );
        if (affected == 0) {
            throw new IllegalArgumentException("商品不存在");
        }
        saveSkus(id, productCode, skus);
        return ApiResponse.success(null);
    }

    @GetMapping("/product-orders")
    public ApiResponse<Map<String, Object>> listProductOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productOrderService.searchOrders(merchantNo, orderNo, status, keyword, createdAtStart, createdAtEnd, page, size));
    }

    @GetMapping("/product-orders/{orderNo}")
    public ApiResponse<Map<String, Object>> getProductOrder(@PathVariable String orderNo) {
        return ApiResponse.success(productOrderService.getOrder(orderNo));
    }

    @PutMapping("/product-orders/{orderNo}/status")
    public ApiResponse<Map<String, Object>> updateProductOrderStatus(
            @PathVariable String orderNo,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        return ApiResponse.success(productOrderService.updateStatus(orderNo, newStatus));
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
                               JSON_UNQUOTE(JSON_EXTRACT(request_payload, '$.customerDisplayName')) AS customer_display_name,
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

    private Map<String, Object> findAddress(Long id) {
        try {
            return mallhomeJdbcTemplate.queryForMap("SELECT id, user_id FROM address WHERE id = ?", id);
        } catch (Exception exception) {
            throw new IllegalArgumentException("地址不存在");
        }
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

    private String generateUniqueProductCode(String merchantNo) {
        for (int i = 0; i < 10; i++) {
            String productCode = "SPU" + System.currentTimeMillis() + secureRandom.nextInt(1000, 10000);
            if (repository.findOne(
                    "SELECT id FROM merchant_product WHERE merchant_no = ? AND product_code = ?",
                    merchantNo,
                    productCode
            ).isEmpty()) {
                return productCode;
            }
        }
        throw new IllegalStateException("商品编码生成失败，请重试");
    }

    private String generateUniqueCategoryCode() {
        for (int i = 0; i < 10; i++) {
            String categoryCode = "CAT" + System.currentTimeMillis() + secureRandom.nextInt(1000, 10000);
            if (repository.findOne("SELECT id FROM product_category WHERE category_code = ?", categoryCode).isEmpty()) {
                return categoryCode;
            }
        }
        throw new IllegalStateException("分类编码生成失败，请重试");
    }

    private String randomUrlToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void ensureAppExists(String appId) {
        if (repository.findOne("SELECT id FROM gateway_app WHERE app_id = ?", appId).isEmpty()) {
            throw new IllegalArgumentException("所属商家不存在");
        }
    }

    private String requiredCategory(Map<String, Object> payload) {
        String category = requiredString(payload, "category", "请选择商品分类");
        if (repository.findOne("SELECT id FROM product_category WHERE category_code = ?", category).isEmpty()) {
            throw new IllegalArgumentException("商品分类不存在");
        }
        return category;
    }

    private List<String> imageUrls(Map<String, Object> payload) {
        Object raw = payload.get("imageUrls");
        List<String> urls = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String url = item == null ? "" : String.valueOf(item).trim();
                if (url != null && !url.isBlank() && urls.size() < 5) {
                    urls.add(url);
                }
            }
        }
        if (urls.isEmpty()) {
            String coverImage = value(payload, "coverImage");
            if (coverImage != null && !coverImage.isBlank()) {
                urls.add(coverImage);
            }
        }
        return urls;
    }

    private String firstImage(List<String> imageUrls) {
        return imageUrls.isEmpty() ? "" : imageUrls.get(0);
    }

    private void attachSkus(Map<String, Object> product) {
        Object productId = product.get("id");
        List<Map<String, Object>> skus = repository.findList(
                """
                        SELECT id, sku_code, sku_name, price, stock, enabled, sort_order
                        FROM merchant_product_sku
                        WHERE product_id = ?
                        ORDER BY sort_order ASC, id ASC
                        """,
                productId
        );
        product.put("skus", skus);
    }

    private List<Map<String, Object>> skuPayloads(Map<String, Object> payload) {
        Object raw = payload.get("skus");
        List<Map<String, Object>> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            int index = 0;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> rawMap)) continue;
                Map<String, Object> map = new LinkedHashMap<>();
                String skuName = rawValue(rawMap, "skuName", rawValue(rawMap, "sku_name", "默认规格"));
                if (skuName.isBlank()) skuName = "默认规格";
                map.put("skuName", skuName);
                map.put("skuCode", rawValue(rawMap, "skuCode", rawValue(rawMap, "sku_code", "")));
                map.put("price", requiredAmount(rawMap.get("price")));
                map.put("stock", nonNegativeInt(rawMap.get("stock"), "SKU库存不能小于0"));
                map.put("enabled", bool(rawMap.get("enabled")) == null || Boolean.TRUE.equals(bool(rawMap.get("enabled"))));
                Object sortOrder = rawMap.get("sortOrder") != null ? rawMap.get("sortOrder") : rawMap.get("sort_order");
                map.put("sortOrder", nonNegativeInt(sortOrder != null ? sortOrder : index, "SKU排序不能小于0"));
                result.add(map);
                index++;
            }
        }
        if (result.isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("skuName", "默认规格");
            map.put("skuCode", "");
            map.put("price", requiredAmount(payload.get("price")));
            map.put("stock", nonNegativeInt(payload.get("stock"), "库存不能小于0"));
            map.put("enabled", true);
            map.put("sortOrder", 0);
            result.add(map);
        }
        return result;
    }

    private BigDecimal skuPrice(List<Map<String, Object>> skus) {
        return (BigDecimal) skus.stream()
                .filter(sku -> Boolean.TRUE.equals(sku.get("enabled")))
                .findFirst()
                .orElse(skus.get(0))
                .get("price");
    }

    private int skuStock(List<Map<String, Object>> skus) {
        Object stock = skus.stream()
                .filter(sku -> Boolean.TRUE.equals(sku.get("enabled")))
                .findFirst()
                .orElse(skus.get(0))
                .get("stock");
        return stock instanceof Number n ? n.intValue() : 0;
    }

    private String rawValue(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private void saveSkus(Long productId, String productCode, List<Map<String, Object>> skus) {
        Set<String> savedSkuCodes = new LinkedHashSet<>();
        int index = 0;
        for (Map<String, Object> sku : skus) {
            String skuCode = String.valueOf(sku.getOrDefault("skuCode", "")).trim();
            if (skuCode.isBlank()) {
                skuCode = productCode + "-SKU" + (index + 1);
            }
            if (!savedSkuCodes.add(skuCode)) {
                throw new IllegalArgumentException("SKU编码不能重复");
            }
            boolean exists = repository.findOne(
                    "SELECT id FROM merchant_product_sku WHERE product_id = ? AND sku_code = ?",
                    productId,
                    skuCode
            ).isPresent();
            if (exists) {
                repository.update(
                        """
                                UPDATE merchant_product_sku
                                SET sku_name = ?, price = ?, stock = ?, enabled = ?, sort_order = ?, updated_at = NOW()
                                WHERE product_id = ? AND sku_code = ?
                                """,
                        sku.get("skuName"),
                        sku.get("price"),
                        sku.get("stock"),
                        sku.get("enabled"),
                        sku.get("sortOrder"),
                        productId,
                        skuCode
                );
            } else {
                repository.update(
                        """
                                INSERT INTO merchant_product_sku (
                                    product_id, sku_code, sku_name, price, stock, enabled, sort_order, created_at, updated_at
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                                """,
                        productId,
                        skuCode,
                        sku.get("skuName"),
                        sku.get("price"),
                        sku.get("stock"),
                        sku.get("enabled"),
                        sku.get("sortOrder")
                );
            }
            index++;
        }
        deleteRemovedSkus(productId, savedSkuCodes);
    }

    private void deleteRemovedSkus(Long productId, Set<String> savedSkuCodes) {
        if (savedSkuCodes.isEmpty()) {
            repository.update("DELETE FROM merchant_product_sku WHERE product_id = ?", productId);
            return;
        }
        String placeholders = String.join(", ", savedSkuCodes.stream().map(code -> "?").toList());
        Object[] args = new Object[savedSkuCodes.size() + 1];
        args[0] = productId;
        int index = 1;
        for (String skuCode : savedSkuCodes) {
            args[index++] = skuCode;
        }
        repository.update(
                "DELETE FROM merchant_product_sku WHERE product_id = ? AND sku_code NOT IN (" + placeholders + ")",
                args
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("图片数据格式不正确");
        }
    }

    private String requiredString(Map<String, Object> payload, String key, String message) {
        String value = value(payload, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
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

    private BigDecimal requiredAmount(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("请输入商品价格");
        }
        BigDecimal amount = new BigDecimal(String.valueOf(value));
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("商品价格不能低于0.01");
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private int nonNegativeInt(Object value, String message) {
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        int number = Integer.parseInt(String.valueOf(value));
        if (number < 0) {
            throw new IllegalArgumentException(message);
        }
        return number;
    }

    private String productStatus(Object value) {
        String status = value == null || String.valueOf(value).isBlank() ? "OFF_SALE" : String.valueOf(value);
        if (!List.of("ON_SALE", "OFF_SALE", "SOLD_OUT").contains(status)) {
            throw new IllegalArgumentException("商品状态不正确");
        }
        return status;
    }

    private static class PageQuery {
        private String sql;
        private final List<Object> args = new ArrayList<>();
    }
}
