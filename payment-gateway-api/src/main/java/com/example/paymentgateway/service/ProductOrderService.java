package com.example.paymentgateway.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductOrderService {

    private final JdbcTemplate jdbc;

    public ProductOrderService(@Qualifier("mallhomeJdbcTemplate") JdbcTemplate mallhomeJdbcTemplate) {
        this.jdbc = mallhomeJdbcTemplate;
    }

    @PostConstruct
    public void ensureDefaultSkus() {
        jdbc.update(
                "UPDATE product_order_item SET sku_name = '默认规格' WHERE sku_name IS NULL OR sku_name = ''"
        );
        jdbc.update(
                "UPDATE product_order_item SET sku_code = 'DEFAULT' WHERE sku_code IS NULL OR sku_code = ''"
        );
    }

    public Map<String, Object> searchOrders(String merchantNo, String orderNo, String status, String keyword,
                                            String createdAtStart, String createdAtEnd, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (merchantNo != null && !merchantNo.isBlank()) {
            where.append(" AND o.merchant_no = ?");
            params.add(merchantNo);
        }
        if (orderNo != null && !orderNo.isBlank()) {
            where.append(" AND o.order_no LIKE ?");
            params.add("%" + orderNo.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND o.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (o.order_no LIKE ? OR o.user_phone LIKE ? OR o.merchant_name LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        addDateRange(where, params, "o.created_at", createdAtStart, createdAtEnd);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        // Count total
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_order o" + where,
                Long.class,
                params.toArray()
        );

        // Query list
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add(safePage * safeSize);

        List<Map<String, Object>> content = jdbc.queryForList(
                "SELECT o.id, o.order_no AS orderNo, o.user_id AS userId, o.user_phone AS userPhone, " +
                "o.user_display_name AS userDisplayName, " +
                "o.merchant_no AS merchantNo, o.merchant_name AS merchantName, " +
                "o.total_amount AS totalAmount, o.discount_amount AS discountAmount, o.shipping_fee AS shippingFee, " +
                "o.status, o.shipping_address AS shippingAddress, o.shipping_name AS shippingName, " +
                "o.shipping_phone AS shippingPhone, o.remark, " +
                "o.paid_at AS paidAt, o.shipped_at AS shippedAt, o.delivered_at AS deliveredAt, " +
                "o.completed_at AS completedAt, o.cancelled_at AS cancelledAt, " +
                "o.created_at AS createdAt, o.updated_at AS updatedAt " +
                "FROM product_order o" + where + " ORDER BY o.id DESC LIMIT ? OFFSET ?",
                listParams.toArray()
        );

        // Attach items for each order
        for (Map<String, Object> order : content) {
            attachItems(order);
            attachPayments(order);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", total == null ? 0 : total);
        return result;
    }

    private void addDateRange(StringBuilder where, List<Object> params, String column, String start, String end) {
        if (start != null && !start.isBlank()) {
            where.append(" AND ").append(column).append(" >= ?");
            params.add(start.replace("T", " "));
        }
        if (end != null && !end.isBlank()) {
            where.append(" AND ").append(column).append(" <= ?");
            params.add(end.replace("T", " "));
        }
    }

    public Map<String, Object> getOrder(String orderNo) {
        Map<String, Object> order = jdbc.queryForMap(
                "SELECT o.id, o.order_no AS orderNo, o.user_id AS userId, o.user_phone AS userPhone, " +
                "o.user_display_name AS userDisplayName, " +
                "o.merchant_no AS merchantNo, o.merchant_name AS merchantName, " +
                "o.total_amount AS totalAmount, o.discount_amount AS discountAmount, o.shipping_fee AS shippingFee, " +
                "o.status, o.shipping_address AS shippingAddress, o.shipping_name AS shippingName, " +
                "o.shipping_phone AS shippingPhone, o.remark, " +
                "o.paid_at AS paidAt, o.shipped_at AS shippedAt, o.delivered_at AS deliveredAt, " +
                "o.completed_at AS completedAt, o.cancelled_at AS cancelledAt, " +
                "o.created_at AS createdAt, o.updated_at AS updatedAt " +
                "FROM product_order o WHERE o.order_no = ?",
                orderNo
        );
        attachItems(order);
        attachPayments(order);
        return order;
    }

    public Map<String, Object> updateStatus(String orderNo, String newStatus) {
        if (!List.of("PENDING", "PAID", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "REFUNDING", "REFUNDED")
                .contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单状态不正确");
        }

        Map<String, Object> order = getOrder(orderNo);

        LocalDateTime now = LocalDateTime.now();
        String timestampColumn = null;
        switch (newStatus) {
            case "PAID" -> timestampColumn = "paid_at";
            case "SHIPPED" -> timestampColumn = "shipped_at";
            case "DELIVERED" -> timestampColumn = "delivered_at";
            case "COMPLETED" -> timestampColumn = "completed_at";
            case "CANCELLED" -> timestampColumn = "cancelled_at";
        }

        if (timestampColumn != null) {
            jdbc.update(
                    "UPDATE product_order SET status = ?, " + timestampColumn + " = ?, updated_at = ? WHERE order_no = ?",
                    newStatus, now, now, orderNo
            );
        } else {
            jdbc.update(
                    "UPDATE product_order SET status = ?, updated_at = ? WHERE order_no = ?",
                    newStatus, now, orderNo
            );
        }

        return getOrder(orderNo);
    }

    private void attachItems(Map<String, Object> order) {
        Object orderId = order.get("id");
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT id, order_id AS orderId, product_id AS productId, " +
                "sku_id AS skuId, sku_code AS skuCode, sku_name AS skuName, " +
                "product_code AS productCode, product_name AS productName, product_image AS productImage, " +
                "unit_price AS unitPrice, quantity, subtotal, created_at AS createdAt " +
                "FROM product_order_item WHERE order_id = ?",
                orderId
        );
        order.put("items", items);
    }

    private void attachPayments(Map<String, Object> order) {
        Object orderId = order.get("id");
        List<Map<String, Object>> payments = jdbc.queryForList(
                "SELECT id, order_no AS orderNo, product_order_id AS productOrderId, " +
                "external_id AS externalId, total_amount AS totalAmount, subject, " +
                "status, created_at AS createdAt, updated_at AS updatedAt " +
                "FROM payment_order WHERE product_order_id = ?",
                orderId
        );
        order.put("payments", payments);
    }
}
