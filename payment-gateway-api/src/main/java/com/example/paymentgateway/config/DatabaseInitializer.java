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
        ensureDefaultClient();
        ensureDefaultZhenbaogeConfig();
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关管理员表'
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关管理员会话表'
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关内部应用表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_client (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    app_id VARCHAR(64) NOT NULL COMMENT '接入应用ID，和 gateway_app.app_id 一一对应',
                    client_name VARCHAR(128) NOT NULL COMMENT '接入方名称',
                    contact_name VARCHAR(64) COMMENT '联系人',
                    contact_phone VARCHAR(32) COMMENT '联系电话',
                    status VARCHAR(32) NOT NULL COMMENT '接入方状态：ACTIVE=正常，DISABLED=停用',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_client_app_id (app_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关接入方表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_client_user (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    client_id BIGINT NOT NULL COMMENT '接入方ID',
                    username VARCHAR(64) NOT NULL COMMENT '登录账号',
                    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
                    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
                    role VARCHAR(32) NOT NULL COMMENT '角色：OWNER=负责人，OPERATOR=操作员，VIEWER=只读',
                    enabled BOOLEAN NOT NULL COMMENT '账号状态：1=true=启用，0=false=禁用',
                    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_client_user_username (username),
                    KEY idx_gateway_client_user_client_id (client_id),
                    CONSTRAINT fk_gateway_client_user_client FOREIGN KEY (client_id) REFERENCES gateway_client(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关接入方后台账号表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_client_session (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    client_user_id BIGINT NOT NULL COMMENT '接入方账号ID',
                    token VARCHAR(128) NOT NULL COMMENT '登录令牌',
                    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_client_session_token (token),
                    KEY idx_gateway_client_session_user_id (client_user_id),
                    KEY idx_gateway_client_session_expires_at (expires_at),
                    CONSTRAINT fk_gateway_client_session_user FOREIGN KEY (client_user_id) REFERENCES gateway_client_user(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关接入方后台会话表'
                """);



        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS zhenbaoge_channel_config (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    config_name VARCHAR(64) NOT NULL COMMENT '配置名称',
                    host VARCHAR(256) NOT NULL COMMENT '支付平台网关地址',
                    external_id VARCHAR(64) NOT NULL COMMENT '平台商户ID',
                    md5_key VARCHAR(128) NOT NULL COMMENT '平台MD5密钥',
                    aes_key VARCHAR(128) NOT NULL COMMENT '平台AES密钥',
                    notify_url VARCHAR(512) NOT NULL COMMENT '平台回调支付网关地址',
                    enabled BOOLEAN NOT NULL COMMENT '渠道状态：1=true=启用，0=false=禁用',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_zhenbaoge_channel_config_name (config_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='珍宝阁支付平台通道配置表'
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
                    platform_trade_no VARCHAR(128) COMMENT '支付平台订单号',
                    alipay_trade_no VARCHAR(128) COMMENT '支付宝交易号',
                    return_url VARCHAR(512) COMMENT '本次支付完成后的第三方同步跳转地址',
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关支付流水表'
                """);
        ensureColumn("gateway_pay_order", "platform_trade_no",
                "ALTER TABLE gateway_pay_order ADD COLUMN platform_trade_no VARCHAR(128) COMMENT '支付平台订单号' AFTER trade_status");
        ensureColumn("gateway_pay_order", "business_notify_url",
                "ALTER TABLE gateway_pay_order ADD COLUMN business_notify_url VARCHAR(512) COMMENT '业务系统支付结果通知地址' AFTER alipay_trade_no");
        ensureColumn("gateway_pay_order", "return_url",
                "ALTER TABLE gateway_pay_order ADD COLUMN return_url VARCHAR(512) COMMENT '本次支付完成后的第三方同步跳转地址' AFTER alipay_trade_no");
        ensureColumn("gateway_pay_order", "business_notify_result",
                "ALTER TABLE gateway_pay_order ADD COLUMN business_notify_result VARCHAR(32) COMMENT '业务通知结果：SUCCESS=通知成功，FAIL=通知失败，SKIPPED=未配置跳过' AFTER notify_payload");
        ensureColumn("gateway_pay_order", "business_notified_at",
                "ALTER TABLE gateway_pay_order ADD COLUMN business_notified_at TIMESTAMP NULL COMMENT '最近一次业务通知时间' AFTER business_notify_result");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_notify_record (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    gateway_order_no VARCHAR(64) COMMENT '网关订单号',
                    merchant_order_no VARCHAR(64) COMMENT '业务商户订单号',
                    platform_trade_no VARCHAR(128) COMMENT '支付平台订单号',
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关渠道回调记录表'
                """);
        ensureColumn("gateway_notify_record", "platform_trade_no",
                "ALTER TABLE gateway_notify_record ADD COLUMN platform_trade_no VARCHAR(128) COMMENT '支付平台订单号' AFTER merchant_order_no");
        jdbcTemplate.update("""
                UPDATE gateway_notify_record n
                JOIN gateway_pay_order o ON o.gateway_order_no = n.gateway_order_no
                SET n.merchant_order_no = COALESCE(NULLIF(n.merchant_order_no, ''), o.merchant_order_no),
                    n.platform_trade_no = COALESCE(NULLIF(n.platform_trade_no, ''), o.platform_trade_no)
                WHERE n.merchant_order_no IS NULL OR n.merchant_order_no = ''
                   OR n.platform_trade_no IS NULL OR n.platform_trade_no = ''
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gateway_refund_order (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    refund_order_no VARCHAR(64) NOT NULL COMMENT '网关退款单号',
                    gateway_order_no VARCHAR(64) NOT NULL COMMENT '网关支付订单号',
                    app_id VARCHAR(64) NOT NULL COMMENT '内部应用ID',
                    merchant_order_no VARCHAR(64) NOT NULL COMMENT '业务商户订单号',
                    platform_trade_no VARCHAR(128) COMMENT '支付平台订单号',
                    refund_amount NUMERIC(18, 2) NOT NULL COMMENT '退款金额',
                    refund_reason VARCHAR(128) NOT NULL COMMENT '退款原因',
                    status VARCHAR(32) NOT NULL COMMENT '退款状态：CREATED=已创建，SUCCESS=退款成功，FAILED=退款失败，PROCESSING=处理中',
                    trade_status VARCHAR(64) COMMENT '平台退款状态',
                    request_payload LONGTEXT COMMENT '退款请求参数',
                    platform_response LONGTEXT COMMENT '平台退款响应',
                    notify_payload LONGTEXT COMMENT '平台退款通知原始内容',
                    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
                    updated_at TIMESTAMP NOT NULL COMMENT '更新时间',
                    UNIQUE KEY uk_gateway_refund_order_no (refund_order_no),
                    KEY idx_gateway_refund_gateway_order_no (gateway_order_no),
                    KEY idx_gateway_refund_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付网关退款流水表'
                """);
        ensureColumn("gateway_refund_order", "platform_trade_no",
                "ALTER TABLE gateway_refund_order ADD COLUMN platform_trade_no VARCHAR(128) COMMENT '支付平台订单号' AFTER merchant_order_no");
        ensureColumn("gateway_refund_order", "trade_status",
                "ALTER TABLE gateway_refund_order ADD COLUMN trade_status VARCHAR(64) COMMENT '平台退款状态' AFTER status");
        ensureColumn("gateway_refund_order", "request_payload",
                "ALTER TABLE gateway_refund_order ADD COLUMN request_payload LONGTEXT COMMENT '退款请求参数' AFTER trade_status");
        ensureColumn("gateway_refund_order", "platform_response",
                "ALTER TABLE gateway_refund_order ADD COLUMN platform_response LONGTEXT COMMENT '平台退款响应' AFTER request_payload");
        ensureColumn("gateway_refund_order", "notify_payload",
                "ALTER TABLE gateway_refund_order ADD COLUMN notify_payload LONGTEXT COMMENT '平台退款通知原始内容' AFTER platform_response");
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

    private void ensureDefaultZhenbaogeConfig() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM zhenbaoge_channel_config", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        GatewayProperties.Zhenbaoge config = properties.getZhenbaoge();
        jdbcTemplate.update(
                """
                        INSERT INTO zhenbaoge_channel_config (
                            config_name, host, external_id, md5_key, aes_key, notify_url,
                            enabled, created_at, updated_at
                        ) VALUES ('default', ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                config.getHost(),
                config.getExternalId(),
                config.getMd5Key(),
                config.getAesKey(),
                config.getNotifyUrl(),
                config.isEnabled()
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

    private void ensureDefaultClient() {
        if (!properties.getDemoApp().isEnabled()) {
            return;
        }
        String appId = properties.getDemoApp().getAppId();
        Integer clientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gateway_client WHERE app_id = ?",
                Integer.class,
                appId
        );
        if (clientCount == null || clientCount == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO gateway_client (
                                app_id, client_name, contact_name, contact_phone, status, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
                            """,
                    appId,
                    properties.getClient().getDefaultName(),
                    "默认联系人",
                    ""
            );
        }
        Long clientId = jdbcTemplate.queryForObject("SELECT id FROM gateway_client WHERE app_id = ?", Long.class, appId);
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gateway_client_user WHERE username = ?",
                Integer.class,
                properties.getClient().getDefaultUsername()
        );
        if (clientId != null && (userCount == null || userCount == 0)) {
            jdbcTemplate.update(
                    """
                            INSERT INTO gateway_client_user (
                                client_id, username, password_hash, display_name, role, enabled, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, 'OWNER', true, NOW(), NOW())
                            """,
                    clientId,
                    properties.getClient().getDefaultUsername(),
                    passwordEncoder.encode(properties.getClient().getDefaultPassword()),
                    properties.getClient().getDefaultName()
            );
        } else if (clientId != null) {
            jdbcTemplate.update(
                    """
                            UPDATE gateway_client_user
                            SET client_id = ?, password_hash = ?, display_name = ?, enabled = true, updated_at = NOW()
                            WHERE username = ?
                            """,
                    clientId,
                    passwordEncoder.encode(properties.getClient().getDefaultPassword()),
                    properties.getClient().getDefaultName(),
                    properties.getClient().getDefaultUsername()
            );
        }
    }

}
