package com.example.mallhome.service;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import com.example.mallhome.util.JsonUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class MerchantService {

    private static final int SESSION_HOURS = 12;
    private static final String PAYMENT_GATEWAY = "PAYMENT_GATEWAY";

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate gatewayJdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public MerchantService(
            JdbcTemplate jdbcTemplate,
            @Qualifier("gatewayProductJdbcTemplate") JdbcTemplate gatewayJdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.gatewayJdbcTemplate = gatewayJdbcTemplate;
    }

    @Transactional
    public Map<String, Object> register(Map<String, Object> payload) {
        String username = requiredPhone(payload, "username", "请输入11位手机号");
        String password = required(payload, "password", "请输入登录密码");
        String merchantName = required(payload, "merchantName", "请输入店铺名称");
        String contactName = value(payload, "contactName");
        String contactPhone = value(payload, "contactPhone");

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchant_user WHERE username = ?",
                Integer.class,
                username
        );
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        String merchantNo = generateUniqueMerchantNo();
        jdbcTemplate.update(
                """
                        INSERT INTO merchant (
                            merchant_no, merchant_name, contact_name, contact_phone, status, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
                        """,
                merchantNo,
                merchantName,
                contactName,
                contactPhone
        );
        Long merchantId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update(
                """
                        INSERT INTO merchant_user (
                            merchant_id, username, password_hash, display_name, role, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, 'OWNER', true, NOW(), NOW())
                        """,
                merchantId,
                username,
                passwordEncoder.encode(password),
                StringUtils.hasText(contactName) ? contactName : merchantName
        );
        ensureGatewayApp(merchantNo, merchantName);
        ensureDefaultPayConfig(merchantNo);
        return login(Map.of("username", username, "password", password));
    }

    @Transactional
    public Map<String, Object> login(Map<String, Object> payload) {
        String username = requiredPhone(payload, "username", "请输入11位手机号");
        String password = required(payload, "password", "请输入登录密码");
        Map<String, Object> user = findOne(
                """
                        SELECT u.id, u.merchant_id, u.username, u.password_hash, u.display_name, u.role, u.enabled,
                               m.merchant_no, m.merchant_name, m.status AS merchant_status
                        FROM merchant_user u
                        JOIN merchant m ON m.id = u.merchant_id
                        WHERE u.username = ?
                        """,
                username
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!Boolean.TRUE.equals(user.get("enabled"))
                || !"ACTIVE".equals(String.valueOf(user.get("merchant_status")))
                || !passwordEncoder.matches(password, String.valueOf(user.get("password_hash")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        jdbcTemplate.update("DELETE FROM merchant_session WHERE expires_at < NOW()");
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        jdbcTemplate.update(
                """
                        INSERT INTO merchant_session (
                            merchant_user_id, token, expires_at, created_at, updated_at
                        ) VALUES (?, ?, ?, NOW(), NOW())
                        """,
                user.get("id"),
                token,
                expiresAt
        );
        jdbcTemplate.update("UPDATE merchant_user SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?", user.get("id"));
        return Map.of("token", token, "expiresAt", expiresAt, "user", merchantView(user));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> requireMerchant(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Map<String, Object> user = findOne(
                """
                        SELECT u.id, u.merchant_id, u.username, u.display_name, u.role, u.enabled,
                               s.expires_at, m.merchant_no, m.merchant_name, m.status AS merchant_status
                        FROM merchant_session s
                        JOIN merchant_user u ON u.id = s.merchant_user_id
                        JOIN merchant m ON m.id = u.merchant_id
                        WHERE s.token = ?
                        """,
                token
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        if (!Boolean.TRUE.equals(user.get("enabled")) || !"ACTIVE".equals(String.valueOf(user.get("merchant_status")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
        }
        return user;
    }

    @Transactional
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            jdbcTemplate.update("DELETE FROM merchant_session WHERE token = ?", token);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listProducts(Map<String, Object> merchant, Map<String, String> params) {
        ensureGatewayAppForMerchant(merchant);
        Query query = new Query("FROM merchant_product WHERE merchant_no = ?");
        query.args.add(merchant.get("merchant_no"));
        addLike(query, "product_code", params.get("productCode"));
        addLike(query, "product_name", params.get("productName"));
        addLike(query, "category", params.get("category"));
        addEquals(query, "status", params.get("status"));
        addDateRange(query, "created_at", params.get("createdAtStart"), params.get("createdAtEnd"));
        int page = safeInt(params.get("page"), 0);
        int size = Math.min(Math.max(safeInt(params.get("size"), 20), 1), 100);
        List<Map<String, Object>> content = gatewayJdbcTemplate.queryForList(
                """
                        SELECT id, merchant_no, product_code, product_name, category, price, stock, status,
                               cover_image, product_images, description, created_at, updated_at
                        """ + query.sql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                append(query.args, size, page * size)
        );
        content.forEach(this::attachSkus);
        Long total = gatewayJdbcTemplate.queryForObject("SELECT COUNT(*) " + query.sql, Long.class, query.args.toArray());
        return Map.of("content", content, "page", page, "size", size, "totalElements", total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProduct(Map<String, Object> merchant, Long id) {
        ensureGatewayAppForMerchant(merchant);
        Map<String, Object> product = gatewayFindOne(
                """
                        SELECT id, merchant_no, product_code, product_name, category, price, stock, status,
                               cover_image, product_images, description, created_at, updated_at
                        FROM merchant_product
                        WHERE id = ? AND merchant_no = ?
                        """,
                id,
                merchant.get("merchant_no")
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
        attachSkus(product);
        return product;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategories() {
        return gatewayJdbcTemplate.queryForList(
                """
                        SELECT id, category_code, category_name, sort_order, enabled
                        FROM product_category
                        WHERE enabled = true
                        ORDER BY sort_order ASC, id ASC
                        """
        );
    }

    @Transactional
    public Map<String, Object> createProduct(Map<String, Object> merchant, Map<String, Object> payload) {
        ensureGatewayAppForMerchant(merchant);
        String merchantNo = String.valueOf(merchant.get("merchant_no"));
        String productCode = generateUniqueProductCode(merchantNo);
        String productName = required(payload, "productName", "请输入商品名称");
        String category = requiredCategory(payload);
        List<Map<String, Object>> skus = skuPayloads(payload);
        BigDecimal price = skuPrice(skus);
        int stock = skuStock(skus);
        String status = productStatus(payload.get("status"));
        List<String> imageUrls = imageUrls(payload);
        gatewayJdbcTemplate.update(
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
                JsonUtils.toJson(imageUrls),
                value(payload, "description")
        );
        Long productId = gatewayFindOne("SELECT id FROM merchant_product WHERE merchant_no = ? AND product_code = ?", merchantNo, productCode)
                .map(row -> Long.valueOf(String.valueOf(row.get("id"))))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "商品创建失败"));
        saveSkus(productId, productCode, skus);
        return Map.of("merchantNo", merchantNo, "productCode", productCode, "productName", productName);
    }

    @Transactional
    public void updateProduct(Map<String, Object> merchant, Long id, Map<String, Object> payload) {
        ensureGatewayAppForMerchant(merchant);
        String merchantNo = String.valueOf(merchant.get("merchant_no"));
        if (gatewayFindOne("SELECT id FROM merchant_product WHERE id = ? AND merchant_no = ?", id, merchantNo).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        }
        int affected = gatewayJdbcTemplate.update(
                """
                        UPDATE merchant_product
                        SET product_name = ?, category = ?, price = ?, stock = ?, status = ?,
                            cover_image = ?, product_images = ?, description = ?, updated_at = NOW()
                        WHERE id = ? AND merchant_no = ?
                        """,
                required(payload, "productName", "请输入商品名称"),
                requiredCategory(payload),
                skuPrice(skuPayloads(payload)),
                skuStock(skuPayloads(payload)),
                productStatus(payload.get("status")),
                firstImage(imageUrls(payload)),
                JsonUtils.toJson(imageUrls(payload)),
                value(payload, "description"),
                id,
                merchantNo
        );
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        }
        String productCode = gatewayFindOne("SELECT product_code FROM merchant_product WHERE id = ?", id)
                .map(row -> String.valueOf(row.get("product_code")))
                .orElse("SPU");
        saveSkus(id, productCode, skuPayloads(payload));
    }

    public Map<String, Object> merchantView(Map<String, Object> user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.get("id"));
        view.put("merchantId", user.get("merchant_id"));
        view.put("merchantNo", user.get("merchant_no"));
        view.put("merchantName", user.get("merchant_name"));
        view.put("username", user.get("username"));
        view.put("displayName", user.get("display_name"));
        view.put("role", user.get("role"));
        return view;
    }

    private Optional<Map<String, Object>> findOne(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private Optional<Map<String, Object>> gatewayFindOne(String sql, Object... args) {
        List<Map<String, Object>> rows = gatewayJdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private void ensureGatewayAppForMerchant(Map<String, Object> merchant) {
        ensureGatewayApp(
                String.valueOf(merchant.get("merchant_no")),
                String.valueOf(merchant.get("merchant_name"))
        );
    }

    private void ensureGatewayApp(String appId, String appName) {
        if (!StringUtils.hasText(appId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商家编号不存在");
        }
        if (gatewayFindOne("SELECT id FROM gateway_app WHERE app_id = ?", appId).isPresent()) {
            gatewayJdbcTemplate.update(
                    """
                            UPDATE gateway_app
                            SET app_name = ?, enabled = true, updated_at = NOW()
                            WHERE app_id = ?
                            """,
                    StringUtils.hasText(appName) ? appName : appId,
                    appId
            );
            return;
        }
        gatewayJdbcTemplate.update(
                """
                        INSERT INTO gateway_app (
                            app_id, app_secret, app_name, enabled, notify_url, ip_whitelist, created_at, updated_at
                        ) VALUES (?, ?, ?, true, NULL, NULL, NOW(), NOW())
                        """,
                appId,
                generateAppSecret(),
                StringUtils.hasText(appName) ? appName : appId
        );
    }

    private void ensureDefaultPayConfig(String merchantNo) {
        String gatewayAppSecret = gatewayFindOne("SELECT app_secret FROM gateway_app WHERE app_id = ?", merchantNo)
                .map(row -> String.valueOf(row.get("app_secret")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "支付网关应用创建失败"));
        Map<String, Object> global = findOne("SELECT * FROM pay_runtime_config ORDER BY id LIMIT 1").orElse(Map.of());
        jdbcTemplate.update(
                """
                        INSERT INTO merchant_pay_config (
                            merchant_no, pay_channel, mallhome_host, external_id, notify_url, return_url,
                            default_pay_method_type, gateway_host, gateway_app_id, gateway_app_secret,
                            gateway_return_url, gateway_business_notify_url, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        ON DUPLICATE KEY UPDATE
                            pay_channel = VALUES(pay_channel),
                            gateway_app_id = VALUES(gateway_app_id),
                            gateway_app_secret = VALUES(gateway_app_secret),
                            updated_at = NOW()
                        """,
                merchantNo,
                PAYMENT_GATEWAY,
                configValue(global, "mallhome_host", ""),
                merchantNo,
                configValue(global, "notify_url", ""),
                configValue(global, "return_url", ""),
                configValue(global, "default_pay_method_type", "ALIPAY"),
                configValue(global, "gateway_host", "http://127.0.0.1:8090"),
                merchantNo,
                gatewayAppSecret,
                configValue(global, "gateway_return_url", ""),
                configValue(global, "gateway_business_notify_url", "https://api.linsy.online/api/merchant/gateway/pay/notify")
        );
    }

    private static String configValue(Map<String, Object> row, String key, String fallback) {
        Object value = row.get(key);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private String generateUniqueMerchantNo() {
        for (int i = 0; i < 10; i++) {
            String no = "M" + System.currentTimeMillis() + secureRandom.nextInt(1000, 10000);
            if (findOne("SELECT id FROM merchant WHERE merchant_no = ?", no).isEmpty()) return no;
        }
        throw new IllegalStateException("商家编号生成失败");
    }

    private String generateUniqueProductCode(String merchantNo) {
        for (int i = 0; i < 10; i++) {
            String code = "SPU" + System.currentTimeMillis() + secureRandom.nextInt(1000, 10000);
            if (gatewayFindOne("SELECT id FROM merchant_product WHERE merchant_no = ? AND product_code = ?", merchantNo, code).isEmpty()) return code;
        }
        throw new IllegalStateException("商品编码生成失败");
    }

    private String generateAppSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "gwsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String required(Map<String, Object> payload, String key, String message) {
        String value = value(payload, key);
        if (!StringUtils.hasText(value)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return value;
    }

    private String requiredPhone(Map<String, Object> payload, String key, String message) {
        String value = required(payload, key, message);
        if (!value.matches("^1\\d{10}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "登录账号必须是11位手机号");
        }
        return value;
    }

    private String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Boolean bool(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private BigDecimal amount(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入商品价格");
        }
        BigDecimal amount = new BigDecimal(String.valueOf(value));
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品价格不能低于0.01");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private int nonNegativeInt(Object value) {
        int number = value == null || !StringUtils.hasText(String.valueOf(value)) ? 0 : Integer.parseInt(String.valueOf(value));
        if (number < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存不能小于0");
        return number;
    }

    private String productStatus(Object value) {
        String status = value == null || !StringUtils.hasText(String.valueOf(value)) ? "OFF_SALE" : String.valueOf(value);
        if (!List.of("ON_SALE", "OFF_SALE", "SOLD_OUT").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品状态不正确");
        }
        return status;
    }

    private String requiredCategory(Map<String, Object> payload) {
        String category = required(payload, "category", "请选择商品分类");
        if (gatewayFindOne("SELECT id FROM product_category WHERE category_code = ?", category).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品分类不存在");
        }
        return category;
    }

    private List<String> imageUrls(Map<String, Object> payload) {
        Object raw = payload.get("imageUrls");
        List<String> urls = new java.util.ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String url = item == null ? "" : String.valueOf(item).trim();
                if (StringUtils.hasText(url) && urls.size() < 5) {
                    urls.add(url);
                }
            }
        }
        if (urls.isEmpty()) {
            String coverImage = value(payload, "coverImage");
            if (StringUtils.hasText(coverImage)) {
                urls.add(coverImage);
            }
        }
        return urls;
    }

    private String firstImage(List<String> imageUrls) {
        return imageUrls.isEmpty() ? "" : imageUrls.get(0);
    }

    private void attachSkus(Map<String, Object> product) {
        List<Map<String, Object>> skus = gatewayJdbcTemplate.queryForList(
                """
                        SELECT id, sku_code, sku_name, price, stock, enabled, sort_order
                        FROM merchant_product_sku
                        WHERE product_id = ?
                        ORDER BY sort_order ASC, id ASC
                        """,
                product.get("id")
        );
        product.put("skus", skus);
    }

    private void addDateRange(Query query, String column, String start, String end) {
        if (StringUtils.hasText(start)) {
            query.sql += " AND " + column + " >= ?";
            query.args.add(start.replace("T", " "));
        }
        if (StringUtils.hasText(end)) {
            query.sql += " AND " + column + " <= ?";
            query.args.add(end.replace("T", " "));
        }
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
                if (!StringUtils.hasText(skuName)) skuName = "默认规格";
                map.put("skuName", skuName);
                map.put("skuCode", rawValue(rawMap, "skuCode", rawValue(rawMap, "sku_code", "")));
                map.put("price", amount(rawMap.get("price")));
                map.put("stock", nonNegativeInt(rawMap.get("stock")));
                Boolean enabled = bool(rawMap.get("enabled"));
                map.put("enabled", enabled == null || enabled);
                map.put("sortOrder", nonNegativeInt(rawMap.get("sortOrder") != null ? rawMap.get("sortOrder") : index));
                result.add(map);
                index++;
            }
        }
        if (result.isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("skuName", "默认规格");
            map.put("skuCode", "");
            map.put("price", amount(payload.get("price")));
            map.put("stock", nonNegativeInt(payload.get("stock")));
            map.put("enabled", true);
            map.put("sortOrder", 0);
            result.add(map);
        }
        return result;
    }

    private String rawValue(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
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

    private void saveSkus(Long productId, String productCode, List<Map<String, Object>> skus) {
        Set<String> savedSkuCodes = new LinkedHashSet<>();
        int index = 0;
        for (Map<String, Object> sku : skus) {
            String skuCode = String.valueOf(sku.getOrDefault("skuCode", "")).trim();
            if (!StringUtils.hasText(skuCode)) {
                skuCode = productCode + "-SKU" + (index + 1);
            }
            if (!savedSkuCodes.add(skuCode)) {
                throw new IllegalArgumentException("SKU编码不能重复");
            }
            Integer exists = gatewayJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM merchant_product_sku WHERE product_id = ? AND sku_code = ?",
                    Integer.class,
                    productId,
                    skuCode
            );
            if (exists != null && exists > 0) {
                gatewayJdbcTemplate.update(
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
                gatewayJdbcTemplate.update(
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
            gatewayJdbcTemplate.update("DELETE FROM merchant_product_sku WHERE product_id = ?", productId);
            return;
        }
        String placeholders = String.join(", ", savedSkuCodes.stream().map(code -> "?").toList());
        Object[] args = new Object[savedSkuCodes.size() + 1];
        args[0] = productId;
        int index = 1;
        for (String skuCode : savedSkuCodes) {
            args[index++] = skuCode;
        }
        gatewayJdbcTemplate.update(
                "DELETE FROM merchant_product_sku WHERE product_id = ? AND sku_code NOT IN (" + placeholders + ")",
                args
        );
    }

    private int safeInt(String value, int fallback) {
        try {
            return value == null ? fallback : Math.max(Integer.parseInt(value), 0);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void addEquals(Query query, String column, String value) {
        if (StringUtils.hasText(value)) {
            query.sql += " AND " + column + " = ?";
            query.args.add(value);
        }
    }

    private void addLike(Query query, String column, String value) {
        if (StringUtils.hasText(value)) {
            query.sql += " AND " + column + " LIKE ?";
            query.args.add("%" + value.trim() + "%");
        }
    }

    private Object[] append(List<Object> args, Object... values) {
        List<Object> all = new java.util.ArrayList<>(args);
        all.addAll(List.of(values));
        return all.toArray();
    }

    private static class Query {
        private String sql;
        private final List<Object> args = new java.util.ArrayList<>();

        private Query(String sql) {
            this.sql = sql;
        }
    }
}
