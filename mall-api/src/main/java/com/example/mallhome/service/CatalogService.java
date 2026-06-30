package com.example.mallhome.service;

import com.example.mallhome.util.JsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogService {

    private final JdbcTemplate gatewayJdbcTemplate;

    public CatalogService(@Qualifier("gatewayProductJdbcTemplate") JdbcTemplate gatewayJdbcTemplate) {
        this.gatewayJdbcTemplate = gatewayJdbcTemplate;
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

    @Transactional(readOnly = true)
    public Map<String, Object> listProducts(String category, String keyword, int page, int size) {
        StringBuilder where = new StringBuilder("""
                FROM merchant_product p
                JOIN gateway_app a ON a.app_id = p.merchant_no AND a.enabled = true
                LEFT JOIN product_category c ON c.category_code = p.category
                WHERE p.status = 'ON_SALE'
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(category) && !"all".equals(category)) {
            where.append(" AND p.category = ?");
            args.add(category.trim());
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (p.product_name LIKE ? OR p.description LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> rows = gatewayJdbcTemplate.queryForList(
                """
                        SELECT p.id, p.merchant_no, p.product_code, p.product_name, p.category,
                               a.app_name,
                               COALESCE(c.category_name, p.category) AS category_name,
                               p.price, p.stock, p.cover_image, p.product_images, p.description, p.updated_at
                        """ + where + " ORDER BY p.updated_at DESC, p.id DESC LIMIT ? OFFSET ?",
                append(args, safeSize, safePage * safeSize)
        );
        List<Map<String, Object>> products = rows.stream().map(this::productView).toList();
        Long total = gatewayJdbcTemplate.queryForObject("SELECT COUNT(*) " + where, Long.class, args.toArray());
        return Map.of(
                "content", products,
                "page", safePage,
                "size", safeSize,
                "totalElements", total == null ? 0 : total
        );
    }

    private Map<String, Object> productView(Map<String, Object> row) {
        List<Map<String, Object>> skus = skuViews(row.get("id"));
        Map<String, Object> defaultSku = skus.isEmpty() ? Map.of() : skus.get(0);
        BigDecimal price = defaultSku.get("price") == null
                ? new BigDecimal(String.valueOf(row.get("price")))
                : new BigDecimal(String.valueOf(defaultSku.get("price")));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", row.get("id"));
        view.put("merchantNo", row.get("merchant_no"));
        view.put("merchantName", row.get("app_name"));
        view.put("productCode", row.get("product_code"));
        view.put("name", row.get("product_name"));
        view.put("category", row.get("category"));
        view.put("categoryName", row.get("category_name"));
        view.put("price", price);
        view.put("originPrice", price);
        view.put("stock", defaultSku.getOrDefault("stock", row.get("stock")));
        view.put("skus", skus);
        List<String> images = images(row);
        view.put("image", images.get(0));
        view.put("images", images);
        view.put("desc", StringUtils.hasText((String) row.get("description")) ? row.get("description") : row.get("product_name"));
        view.put("sales", 0);
        view.put("rating", 4.8);
        view.put("fast", false);
        view.put("tags", List.of(StringUtils.hasText((String) row.get("app_name")) ? row.get("app_name") : row.get("merchant_no")));
        return view;
    }

    private List<Map<String, Object>> skuViews(Object productId) {
        List<Map<String, Object>> rows = gatewayJdbcTemplate.queryForList(
                """
                        SELECT id, sku_code, sku_name, price, stock, enabled, sort_order
                        FROM merchant_product_sku
                        WHERE product_id = ? AND enabled = true
                        ORDER BY sort_order ASC, id ASC
                        """,
                productId
        );
        return rows.stream().map(row -> {
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("id", row.get("id"));
            sku.put("skuCode", row.get("sku_code"));
            sku.put("skuName", row.get("sku_name"));
            sku.put("price", row.get("price"));
            sku.put("stock", row.get("stock"));
            sku.put("enabled", row.get("enabled"));
            return sku;
        }).toList();
    }

    private String image(Map<String, Object> row) {
        String cover = (String) row.get("cover_image");
        if (StringUtils.hasText(cover)) {
            return cover;
        }
        return "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=900&q=80";
    }

    private List<String> images(Map<String, Object> row) {
        String rawImages = (String) row.get("product_images");
        List<String> images = new ArrayList<>();
        if (StringUtils.hasText(rawImages)) {
            try {
                JsonUtils.readTree(rawImages).forEach(node -> {
                    String url = node.asText("");
                    if (StringUtils.hasText(url) && images.size() < 5) {
                        images.add(url);
                    }
                });
            } catch (Exception ignored) {
                // 兼容历史异常数据，回退到主图。
            }
        }
        if (images.isEmpty()) {
            images.add(image(row));
        }
        return images;
    }

    private Object[] append(List<Object> args, Object... values) {
        List<Object> all = new ArrayList<>(args);
        all.addAll(List.of(values));
        return all.toArray();
    }
}
