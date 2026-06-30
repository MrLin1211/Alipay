package com.example.mallhome.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatewayProductMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate gatewayJdbcTemplate;

    public GatewayProductMigrationRunner(@Qualifier("gatewayProductJdbcTemplate") JdbcTemplate gatewayJdbcTemplate) {
        this.gatewayJdbcTemplate = gatewayJdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        gatewayJdbcTemplate.execute("""
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
        gatewayJdbcTemplate.execute("""
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
        gatewayJdbcTemplate.execute("""
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
        gatewayJdbcTemplate.execute("""
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
    }

    private void seedDefaultCategories() {
        Integer count = gatewayJdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_category", Integer.class);
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
            gatewayJdbcTemplate.update(
                    """
                            INSERT INTO product_category (
                                category_code, category_name, sort_order, enabled, created_at, updated_at
                            ) VALUES (?, ?, ?, true, NOW(), NOW())
                            """,
                    row
            );
        }
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = gatewayJdbcTemplate.queryForObject(
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
            gatewayJdbcTemplate.execute(ddl);
        }
    }

    private void migrateMerchantProductMerchantNo() {
        ensureColumn("merchant_product", "merchant_no",
                "ALTER TABLE merchant_product ADD COLUMN merchant_no VARCHAR(64) COMMENT '所属商家编号' AFTER id");
        if (columnExists("merchant_product", "app_id")) {
            gatewayJdbcTemplate.update("UPDATE merchant_product SET merchant_no = app_id WHERE merchant_no IS NULL OR merchant_no = ''");
        }
        gatewayJdbcTemplate.execute("UPDATE merchant_product SET merchant_no = 'biz-demo' WHERE merchant_no IS NULL OR merchant_no = ''");
        gatewayJdbcTemplate.execute("ALTER TABLE merchant_product MODIFY merchant_no VARCHAR(64) NOT NULL COMMENT '所属商家编号'");
        dropIndexIfExists("merchant_product", "uk_merchant_product_app_code");
        dropIndexIfExists("merchant_product", "idx_merchant_product_app_id");
        addIndexIfMissing("merchant_product", "uk_merchant_product_merchant_code",
                "ALTER TABLE merchant_product ADD UNIQUE KEY uk_merchant_product_merchant_code (merchant_no, product_code)");
        addIndexIfMissing("merchant_product", "idx_merchant_product_merchant_no",
                "ALTER TABLE merchant_product ADD KEY idx_merchant_product_merchant_no (merchant_no)");
        if (columnExists("merchant_product", "app_id")) {
            gatewayJdbcTemplate.execute("ALTER TABLE merchant_product DROP COLUMN app_id");
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = gatewayJdbcTemplate.queryForObject(
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
        Integer count = gatewayJdbcTemplate.queryForObject(
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
            gatewayJdbcTemplate.execute(ddl);
        }
    }

    private void dropIndexIfExists(String tableName, String indexName) {
        Integer count = gatewayJdbcTemplate.queryForObject(
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
            gatewayJdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
        }
    }

    private void ensureDefaultSkus() {
        gatewayJdbcTemplate.update("""
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
}
