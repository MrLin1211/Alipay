package com.example.paymentgateway.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final GatewayProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, GatewayProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        ensureDefaultAdmin();
        ensureDemoApp();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_admin_user (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    username VARCHAR(64) NOT NULL COMMENT '登录账号',
                    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
                    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
                    role VARCHAR(32) NOT NULL COMMENT '角色：SUPER_ADMIN=超级管理员，OPERATOR=运营，VIEWER=只读',
                    enabled BOOLEAN NOT NULL COMMENT '账号状态：1=true=启用，0=false=禁用',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_admin_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付网关管理员表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_admin_session (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    admin_user_id BIGINT NOT NULL COMMENT '管理员ID',
                    token VARCHAR(128) NOT NULL COMMENT '登录令牌',
                    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_admin_session_token (token),
                    KEY idx_gateway_admin_session_expires_at (expires_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付网关管理员会话表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_app (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    app_id VARCHAR(64) NOT NULL COMMENT '内部应用ID',
                    app_secret VARCHAR(128) NOT NULL COMMENT '内部应用密钥',
                    app_name VARCHAR(128) NOT NULL COMMENT '内部应用名称',
                    enabled BOOLEAN NOT NULL COMMENT '应用状态：1=true=启用，0=false=禁用',
                    notify_url VARCHAR(512) COMMENT '业务系统支付结果通知地址',
                    ip_whitelist VARCHAR(1024) COMMENT 'IP白名单，多个用逗号分隔',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_app_app_id (app_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付网关内部应用表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS product_category (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
                    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
                    sort_order INT NOT NULL COMMENT '排序值',
                    enabled BOOLEAN NOT NULL COMMENT '是否启用',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_product_category_code (category_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表'
                """);

        seedDefaultCategories();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS merchant_product (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    merchant_no VARCHAR(64) NOT NULL COMMENT '所属商家编号',
                    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
                    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
                    category VARCHAR(64) COMMENT '商品分类',
                    price NUMERIC(18, 2) NOT NULL COMMENT '销售价',
                    stock INT NOT NULL COMMENT '库存',
                    status VARCHAR(32) NOT NULL COMMENT '状态：ON_SALE=上架，OFF_SALE=下架，SOLD_OUT=售罄',
                    cover_image VARCHAR(512) COMMENT '商品主图',
                    product_images LONGTEXT COMMENT '商品图片JSON数组，最多5张',
                    description VARCHAR(1024) COMMENT '商品描述',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_merchant_product_merchant_code (merchant_no, product_code),
                    KEY idx_merchant_product_merchant_no (merchant_no),
                    KEY idx_merchant_product_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家商品表'
                """);
        migrateMerchantProductMerchantNo();
        ensureColumn("merchant_product", "product_images",
                "ALTER TABLE merchant_product ADD COLUMN product_images LONGTEXT COMMENT '商品图片JSON数组，最多5张' AFTER cover_image");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS merchant_product_sku (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    product_id BIGINT NOT NULL COMMENT '商品ID',
                    sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码',
                    sku_name VARCHAR(128) NOT NULL COMMENT 'SKU名称',
                    price NUMERIC(18, 2) NOT NULL COMMENT 'SKU销售价',
                    stock INT NOT NULL COMMENT 'SKU库存',
                    enabled BOOLEAN NOT NULL COMMENT '是否启用',
                    sort_order INT NOT NULL COMMENT '排序值',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_product_sku_code (product_id, sku_code),
                    KEY idx_product_sku_product_id (product_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SKU表'
                """);
        ensureDefaultSkus();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS alipay_channel_config (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    config_name VARCHAR(64) NOT NULL COMMENT '配置名称',
                    app_id VARCHAR(64) NOT NULL COMMENT '支付宝应用ID',
                    gateway_url VARCHAR(256) NOT NULL COMMENT '支付宝网关地址',
                    app_private_key LONGTEXT NOT NULL COMMENT '应用私钥PKCS8',
                    alipay_public_key LONGTEXT NOT NULL COMMENT '支付宝公钥',
                    notify_url VARCHAR(512) NOT NULL COMMENT '支付宝异步通知地址',
                    return_url VARCHAR(512) COMMENT '支付宝同步跳转地址',
                    sign_type VARCHAR(16) NOT NULL COMMENT '签名类型：RSA2',
                    charset_name VARCHAR(32) NOT NULL COMMENT '字符集：utf-8',
                    enabled BOOLEAN NOT NULL COMMENT '渠道状态：1=true=启用，0=false=禁用',
                    sandbox BOOLEAN NOT NULL COMMENT '是否沙箱：1=true=沙箱，0=false=正式',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_alipay_channel_config_name (config_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付宝渠道配置表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_pay_order (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    gateway_order_no VARCHAR(64) NOT NULL COMMENT '网关订单号',
                    app_id VARCHAR(64) NOT NULL COMMENT '内部应用ID',
                    merchant_order_no VARCHAR(64) NOT NULL COMMENT '业务商户订单号',
                    channel VARCHAR(32) NOT NULL COMMENT '支付渠道：ALIPAY',
                    product_code VARCHAR(64) NOT NULL COMMENT '支付宝产品码：QUICK_WAP_WAY=手机网站支付',
                    subject VARCHAR(128) NOT NULL COMMENT '订单标题',
                    total_amount NUMERIC(18, 2) NOT NULL COMMENT '订单金额',
                    status VARCHAR(32) NOT NULL COMMENT '网关订单状态：CREATED=已创建，PAYING=待支付，SUCCESS=交易成功，FINISHED=交易结束，CLOSED=交易关闭，FAILED=失败，UNKNOWN=未知',
                    trade_status VARCHAR(64) COMMENT '渠道交易状态：TRADE_SUCCESS=支付成功，TRADE_FINISHED=交易结束，TRADE_CLOSED=交易关闭',
                    alipay_trade_no VARCHAR(128) COMMENT '支付宝交易号',
                    business_notify_url VARCHAR(512) COMMENT '业务系统支付结果通知地址',
                    pay_form LONGTEXT COMMENT '支付宝支付表单HTML',
                    request_payload LONGTEXT COMMENT '内部请求原始内容',
                    channel_response LONGTEXT COMMENT '渠道响应原始内容',
                    notify_payload LONGTEXT COMMENT '最近一次渠道通知原始内容',
                    business_notify_result VARCHAR(32) COMMENT '业务通知结果：SUCCESS=通知成功，FAIL=通知失败，SKIPPED=未配置跳过',
                    business_notified_at TIMESTAMP NULL COMMENT '最近一次业务通知时间',
                    paid_at TIMESTAMP NULL COMMENT '交易成功时间',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_order_no (gateway_order_no),
                    UNIQUE KEY uk_gateway_app_merchant_order (app_id, merchant_order_no),
                    KEY idx_gateway_pay_order_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付网关支付流水表'
                """);
        ensureColumn("gateway_pay_order", "business_notify_url",
                "ALTER TABLE gateway_pay_order ADD COLUMN business_notify_url VARCHAR(512) COMMENT '业务系统支付结果通知地址' AFTER alipay_trade_no");
        ensureColumn("gateway_pay_order", "business_notify_result",
                "ALTER TABLE gateway_pay_order ADD COLUMN business_notify_result VARCHAR(32) COMMENT '业务通知结果：SUCCESS=通知成功，FAIL=通知失败，SKIPPED=未配置跳过' AFTER notify_payload");
        ensureColumn("gateway_pay_order", "business_notified_at",
                "ALTER TABLE gateway_pay_order ADD COLUMN business_notified_at TIMESTAMP NULL COMMENT '最近一次业务通知时间' AFTER business_notify_result");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_notify_record (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    gateway_order_no VARCHAR(64) COMMENT '网关订单号',
                    merchant_order_no VARCHAR(64) COMMENT '业务商户订单号',
                    channel VARCHAR(32) NOT NULL COMMENT '支付渠道：ALIPAY',
                    channel_trade_no VARCHAR(128) COMMENT '渠道交易号',
                    trade_status VARCHAR(64) COMMENT '渠道交易状态：TRADE_SUCCESS=支付成功，TRADE_FINISHED=交易结束，TRADE_CLOSED=交易关闭',
                    notify_key VARCHAR(255) NOT NULL COMMENT '通知幂等唯一键',
                    verified BOOLEAN NOT NULL COMMENT '验签结果：1=true=通过，0=false=失败',
                    result VARCHAR(32) NOT NULL COMMENT '处理结果：SUCCESS=处理成功，FAIL=处理失败',
                    failure_reason VARCHAR(512) COMMENT '失败原因',
                    notify_payload LONGTEXT COMMENT '渠道通知原始内容',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_notify_key (notify_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付网关渠道回调记录表'
                """);
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void migrateMerchantProductMerchantNo() {
        ensureColumn("merchant_product", "merchant_no",
                "ALTER TABLE merchant_product ADD COLUMN merchant_no VARCHAR(64) COMMENT '所属商家编号' AFTER id");
        if (columnExists("merchant_product", "app_id")) {
            jdbcTemplate.update("UPDATE merchant_product SET merchant_no = app_id WHERE merchant_no IS NULL OR merchant_no = ''");
        }
        jdbcTemplate.execute("UPDATE merchant_product SET merchant_no = 'biz-demo' WHERE merchant_no IS NULL OR merchant_no = ''");
        jdbcTemplate.execute("ALTER TABLE merchant_product MODIFY merchant_no VARCHAR(64) NOT NULL COMMENT '所属商家编号'");
        dropIndexIfExists("merchant_product", "uk_merchant_product_app_code");
        dropIndexIfExists("merchant_product", "idx_merchant_product_app_id");
        addIndexIfMissing("merchant_product", "uk_merchant_product_merchant_code",
                "ALTER TABLE merchant_product ADD UNIQUE KEY uk_merchant_product_merchant_code (merchant_no, product_code)");
        addIndexIfMissing("merchant_product", "idx_merchant_product_merchant_no",
                "ALTER TABLE merchant_product ADD KEY idx_merchant_product_merchant_no (merchant_no)");
        if (columnExists("merchant_product", "app_id")) {
            jdbcTemplate.execute("ALTER TABLE merchant_product DROP COLUMN app_id");
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private void addIndexIfMissing(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND INDEX_NAME = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void dropIndexIfExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND INDEX_NAME = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
        }
    }

    private void ensureDefaultSkus() {
        jdbcTemplate.update("""
                INSERT INTO merchant_product_sku (
                    product_id, sku_code, sku_name, price, stock, enabled, sort_order, created_at, updated_at
                )
                SELECT p.id, CONCAT(p.product_code, '-DEFAULT'), '默认规格', p.price, p.stock, true, 0, NOW(), NOW()
                FROM merchant_product p
                WHERE NOT EXISTS (
                    SELECT 1 FROM merchant_product_sku s WHERE s.product_id = p.id
                )
                """);
    }

    private void ensureDefaultAdmin() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gateway_admin_user", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        INSERT INTO gateway_admin_user (
                            username, password_hash, display_name, role, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                properties.getAdmin().getDefaultUsername(),
                passwordEncoder.encode(properties.getAdmin().getDefaultPassword()),
                "默认管理员",
                "SUPER_ADMIN",
                true
        );
    }

    private void ensureDemoApp() {
        if (!properties.getDemoApp().isEnabled()) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gateway_app WHERE app_id = ?", Integer.class,
                properties.getDemoApp().getAppId());
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        INSERT INTO gateway_app (
                            app_id, app_secret, app_name, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, NOW(), NOW())
                        """,
                properties.getDemoApp().getAppId(),
                properties.getDemoApp().getAppSecret(),
                "本地演示业务系统",
                true
        );
    }

    private void seedDefaultCategories() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_category", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        Object[][] rows = {
                {"digital", "数码家电", 10},
                {"fashion", "服饰美妆", 20},
                {"fresh", "生鲜食品", 30},
                {"home", "家居日用", 40},
                {"sports", "运动户外", 50},
                {"baby", "母婴玩具", 60}
        };
        for (Object[] row : rows) {
            jdbcTemplate.update(
                    """
                            INSERT INTO product_category (
                                category_code, category_name, sort_order, enabled, created_at, updated_at
                            ) VALUES (?, ?, ?, true, NOW(), NOW())
                            """,
                    row
            );
        }
    }
}
