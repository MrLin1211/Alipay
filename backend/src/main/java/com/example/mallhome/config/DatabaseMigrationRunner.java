package com.example.mallhome.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final MallhomePayProperties payProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate, MallhomePayProperties payProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.payProperties = payProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensurePaymentNotifyKeyColumn();
        ensurePaymentNotifyKeyIndex();
        ensureAdminTables();
        ensureDefaultAdminUser();
        ensurePaymentRefundTable();
        ensurePayRuntimeConfigTable();
        ensureDefaultPayRuntimeConfig();
        migratePaymentOrderSuccessStatus();
        ensureDatabaseComments();
    }

    private void ensurePaymentNotifyKeyColumn() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'payment_notify_record'
                          AND COLUMN_NAME = 'notify_key'
                        """,
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        // 兼容已存在的本地表：先允许为空，再给历史数据填充幂等键。
        jdbcTemplate.execute("ALTER TABLE payment_notify_record ADD COLUMN notify_key VARCHAR(255)");
        jdbcTemplate.execute("""
                UPDATE payment_notify_record
                SET notify_key = CONCAT(
                    COALESCE(order_no, 'OLD'),
                    '|',
                    COALESCE(platform_out_trade_no, '-'),
                    '|',
                    COALESCE(trade_status, '-'),
                    '|',
                    id
                )
                WHERE notify_key IS NULL
                """);
    }

    private void ensurePaymentNotifyKeyIndex() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'payment_notify_record'
                          AND INDEX_NAME = 'uk_payment_notify_key'
                        """,
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE payment_notify_record ADD UNIQUE KEY uk_payment_notify_key (notify_key)");
    }

    private void ensureAdminTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS admin_user (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(64) NOT NULL,
                    password_hash VARCHAR(100) NOT NULL,
                    display_name VARCHAR(64) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    last_login_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    UNIQUE KEY uk_admin_user_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS admin_session (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    admin_user_id BIGINT NOT NULL,
                    token VARCHAR(128) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    UNIQUE KEY uk_admin_session_token (token),
                    KEY idx_admin_session_user_id (admin_user_id),
                    KEY idx_admin_session_expires_at (expires_at),
                    CONSTRAINT fk_admin_session_user FOREIGN KEY (admin_user_id) REFERENCES admin_user(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);
    }

    private void ensureDefaultAdminUser() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_user", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                        INSERT INTO admin_user (
                            username,
                            password_hash,
                            display_name,
                            enabled,
                            created_at,
                            updated_at
                        ) VALUES (?, ?, ?, ?, NOW(), NOW())
                        """,
                "admin",
                passwordEncoder.encode("admin123456"),
                "默认管理员",
                true
        );
    }

    private void ensurePaymentRefundTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS payment_refund (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    request_no VARCHAR(64) NOT NULL,
                    order_no VARCHAR(64) NOT NULL,
                    plat_trade_no VARCHAR(128) NOT NULL,
                    refund_amount NUMERIC(18, 2) NOT NULL,
                    refund_reason VARCHAR(256) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    trade_status VARCHAR(64),
                    platform_response LONGTEXT,
                    created_by VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    UNIQUE KEY uk_payment_refund_request_no (request_no),
                    KEY idx_payment_refund_order_no (order_no),
                    KEY idx_payment_refund_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);
    }

    private void ensurePayRuntimeConfigTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pay_runtime_config (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    pay_channel VARCHAR(32) NOT NULL,
                    mallhome_host VARCHAR(256) NOT NULL,
                    external_id VARCHAR(64) NOT NULL,
                    notify_url VARCHAR(512) NOT NULL,
                    return_url VARCHAR(512),
                    default_pay_method_type VARCHAR(32) NOT NULL,
                    gateway_host VARCHAR(256) NOT NULL,
                    gateway_app_id VARCHAR(128) NOT NULL,
                    gateway_app_secret VARCHAR(256) NOT NULL,
                    gateway_return_url VARCHAR(512),
                    gateway_business_notify_url VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);
    }

    private void ensureDefaultPayRuntimeConfig() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_runtime_config", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        INSERT INTO pay_runtime_config (
                            pay_channel, mallhome_host, external_id, notify_url, return_url,
                            default_pay_method_type, gateway_host, gateway_app_id, gateway_app_secret,
                            gateway_return_url, gateway_business_notify_url, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                "MALLHOME",
                payProperties.getHost(),
                payProperties.getExternalId(),
                payProperties.getNotifyUrl(),
                payProperties.getReturnUrl(),
                payProperties.getDefaultPayMethodType(),
                payProperties.getGatewayHost(),
                payProperties.getGatewayAppId(),
                payProperties.getGatewayAppSecret(),
                payProperties.getReturnUrl(),
                payProperties.getNotifyUrl()
        );
    }

    private void migratePaymentOrderSuccessStatus() {
        // 本地订单成功状态由 PAID 统一调整为 SUCCESS，兼容已有历史订单。
        jdbcTemplate.update("UPDATE payment_order SET status = 'SUCCESS' WHERE status = 'PAID'");
        // FINISHED 属于失败类交易结束状态，不应保留支付成功时间。
        jdbcTemplate.update("UPDATE payment_order SET paid_at = NULL WHERE status = 'FINISHED'");
    }

    private void ensureDatabaseComments() {
        jdbcTemplate.execute("ALTER TABLE payment_order COMMENT = '支付订单表'");
        applyColumnComments("payment_order", new String[][]{
                {"id", "主键ID"}, {"order_no", "商户订单号"}, {"external_id", "平台商户号"},
                {"total_amount", "订单总金额"}, {"subject", "订单标题"}, {"client_ip", "用户客户端IP"},
                {"type_index", "支付类型：1=手机网站，2=电脑网站，3=应用内支付，4=小程序，5=公众号"},
                {"goods_type", "商品类型：允许1-9，当前默认1，具体含义以平台定义为准"},
                {"pay_method_type", "支付方式：ALIPAY_CN=支付宝，ALIPAY=国际支付宝，WECHATPAY=微信支付，CARD=银行卡"},
                {"attach_info", "商户附加信息"},
                {"return_url", "支付完成前端跳转地址"}, {"quit_url", "退出支付跳转地址"},
                {"sub_external_id", "子商户号"},
                {"status", "本地订单状态：CREATED=订单已创建，CREATE_SUCCESS=下单成功，CREATE_FAILED=下单失败，SUCCESS=交易成功，FINISHED=交易结束，CLOSED=交易关闭，UNKNOWN_NOTIFY=未知通知状态"},
                {"trade_status", "平台交易状态：TRADE_SUCCESS=支付成功，TRADE_FINISHED=交易结束，TRADE_CLOSED=交易关闭"},
                {"plat_trade_no", "平台交易单号"},
                {"third_out_trade_no", "第三方交易单号"}, {"pay_url", "平台支付链接"},
                {"evoke_mode", "支付唤起模式：0=跳转链接，1=表单提交，2=二维码"},
                {"platform_create_response", "平台创建订单原始响应"},
                {"notify_payload", "最近一次支付通知原始参数"}, {"paid_at", "交易成功时间"},
                {"created_at", "创建时间"}, {"updated_at", "更新时间"}
        });

        jdbcTemplate.execute("ALTER TABLE payment_notify_record COMMENT = '支付通知记录表'");
        applyColumnComments("payment_notify_record", new String[][]{
                {"id", "主键ID"}, {"order_no", "商户订单号"}, {"external_id", "平台商户号"},
                {"trade_status", "平台交易状态：TRADE_SUCCESS=支付成功，TRADE_FINISHED=交易结束，TRADE_CLOSED=交易关闭"},
                {"platform_out_trade_no", "平台交易单号"},
                {"notify_key", "通知幂等唯一键"}, {"verified", "验签结果：1=true=通过，0=false=失败"},
                {"result", "通知处理结果：SUCCESS=处理成功，FAIL=处理失败"},
                {"failure_reason", "通知处理失败原因"},
                {"notify_payload", "平台通知原始参数"}, {"created_at", "创建时间"},
                {"updated_at", "更新时间"}
        });

        jdbcTemplate.execute("ALTER TABLE admin_user COMMENT = '管理后台账号表'");
        applyColumnComments("admin_user", new String[][]{
                {"id", "主键ID"}, {"username", "登录账号"}, {"password_hash", "BCrypt密码摘要"},
                {"display_name", "管理员显示名称"}, {"enabled", "账号状态：1=true=启用，0=false=禁用"},
                {"last_login_at", "最后登录时间"}, {"created_at", "创建时间"},
                {"updated_at", "更新时间"}
        });

        jdbcTemplate.execute("ALTER TABLE admin_session COMMENT = '管理后台登录会话表'");
        applyColumnComments("admin_session", new String[][]{
                {"id", "主键ID"}, {"admin_user_id", "管理员账号ID"}, {"token", "登录会话令牌"},
                {"expires_at", "会话过期时间"}, {"created_at", "创建时间"},
                {"updated_at", "更新时间"}
        });

        jdbcTemplate.execute("ALTER TABLE payment_refund COMMENT = '退款记录表'");
        applyColumnComments("payment_refund", new String[][]{
                {"id", "主键ID"}, {"request_no", "本地退款请求单号"}, {"order_no", "商户订单号"},
                {"plat_trade_no", "平台交易单号"}, {"refund_amount", "退款金额"},
                {"refund_reason", "退款原因"},
                {"status", "本地退款状态：PROCESSING=处理中，SUCCESS=退款成功，FAILED=退款失败"},
                {"trade_status", "平台退款交易状态：保存平台返回的原始枚举值"},
                {"platform_response", "平台退款原始响应"},
                {"created_by", "提交退款的管理员账号"}, {"created_at", "创建时间"},
                {"updated_at", "更新时间"}
        });

        jdbcTemplate.execute("ALTER TABLE pay_runtime_config COMMENT = '支付运行时配置表'");
        applyColumnComments("pay_runtime_config", new String[][]{
                {"id", "主键ID"},
                {"pay_channel", "支付通道：MALLHOME=原Mallhome平台，PAYMENT_GATEWAY=自建支付网关"},
                {"mallhome_host", "Mallhome平台接口地址"},
                {"external_id", "Mallhome平台商户号"},
                {"notify_url", "Mallhome支付通知地址"},
                {"return_url", "Mallhome同步跳转地址"},
                {"default_pay_method_type", "Mallhome默认支付方式：ALIPAY_CN=支付宝，ALIPAY=国际支付宝，WECHATPAY=微信支付，CARD=银行卡"},
                {"gateway_host", "自建支付网关地址"},
                {"gateway_app_id", "自建支付网关接入应用AppId"},
                {"gateway_app_secret", "自建支付网关接入应用AppSecret"},
                {"gateway_return_url", "自建支付网关同步跳转地址"},
                {"gateway_business_notify_url", "自建支付网关回调业务系统地址"},
                {"created_at", "创建时间"},
                {"updated_at", "更新时间"}
        });
    }

    private void applyColumnComments(String tableName, String[][] comments) {
        for (String[] item : comments) {
            String columnName = item[0];
            String comment = item[1];
            Map<String, Object> definition = jdbcTemplate.queryForMap(
                    """
                            SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
                            FROM information_schema.COLUMNS
                            WHERE TABLE_SCHEMA = DATABASE()
                              AND TABLE_NAME = ?
                              AND COLUMN_NAME = ?
                            """,
                    tableName,
                    columnName
            );
            StringBuilder sql = new StringBuilder("ALTER TABLE `")
                    .append(tableName)
                    .append("` MODIFY COLUMN `")
                    .append(columnName)
                    .append("` ")
                    .append(definition.get("COLUMN_TYPE"))
                    .append("NO".equals(definition.get("IS_NULLABLE")) ? " NOT NULL" : " NULL");

            Object defaultValue = definition.get("COLUMN_DEFAULT");
            if (defaultValue != null) {
                String value = defaultValue.toString();
                if ("CURRENT_TIMESTAMP".equalsIgnoreCase(value)
                        || value.toUpperCase().startsWith("CURRENT_TIMESTAMP(")) {
                    sql.append(" DEFAULT ").append(value);
                } else {
                    sql.append(" DEFAULT ").append(quoteSql(value));
                }
            }

            String extra = String.valueOf(definition.get("EXTRA"));
            if (!extra.isBlank() && !"null".equalsIgnoreCase(extra)) {
                sql.append(" ").append(extra);
            }
            sql.append(" COMMENT ").append(quoteSql(comment));
            jdbcTemplate.execute(sql.toString());
        }
    }

    private static String quoteSql(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
