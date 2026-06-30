package com.example.mallhome.service;

import com.example.mallhome.domain.ProductOrderItemView;
import com.example.mallhome.domain.ProductOrderView;
import com.example.mallhome.domain.PaymentOrderView;
import com.example.mallhome.entity.ProductOrder;
import com.example.mallhome.entity.ProductOrderItem;
import com.example.mallhome.repository.PaymentOrderRepository;
import com.example.mallhome.repository.ProductOrderItemRepository;
import com.example.mallhome.repository.ProductOrderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductOrderService {

    private final ProductOrderRepository productOrderRepository;
    private final ProductOrderItemRepository productOrderItemRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final JdbcTemplate gatewayJdbcTemplate;

    public ProductOrderService(ProductOrderRepository productOrderRepository,
                               ProductOrderItemRepository productOrderItemRepository,
                               PaymentOrderRepository paymentOrderRepository,
                               @Qualifier("gatewayProductJdbcTemplate") JdbcTemplate gatewayJdbcTemplate) {
        this.productOrderRepository = productOrderRepository;
        this.productOrderItemRepository = productOrderItemRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.gatewayJdbcTemplate = gatewayJdbcTemplate;
    }

    @Transactional
    public ProductOrderView createOrder(Long userId, String userPhone, String userDisplayName,
                                         String merchantNo, String merchantName,
                                         BigDecimal totalAmount, BigDecimal discountAmount,
                                         List<Map<String, Object>> items,
                                         String shippingAddress, String shippingName, String shippingPhone,
                                         String remark) {
        String resolvedShippingAddress = requiredShipping(shippingAddress, "请选择收货地址");
        String resolvedShippingName = requiredShipping(shippingName, "收货人不能为空");
        String resolvedShippingPhone = requiredShipping(shippingPhone, "收货电话不能为空");
        ProductOrder order = new ProductOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setUserPhone(userPhone);
        order.setUserDisplayName(userDisplayName);
        List<ResolvedOrderItem> resolvedItems = resolveItems(items);
        String resolvedMerchantNo = resolvedItems.get(0).merchantNo();
        if (merchantNo != null && !merchantNo.isBlank() && !resolvedMerchantNo.equals(merchantNo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品归属商家不一致");
        }

        BigDecimal itemTotal = resolvedItems.stream()
                .map(ResolvedOrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        order.setMerchantNo(resolvedMerchantNo);
        order.setMerchantName(resolvedItems.get(0).merchantName());
        order.setTotalAmount(itemTotal);
        order.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setStatus("PENDING");
        order.setShippingAddress(resolvedShippingAddress);
        order.setShippingName(resolvedShippingName);
        order.setShippingPhone(resolvedShippingPhone);
        order.setRemark(remark);

        productOrderRepository.save(order);

        for (ResolvedOrderItem item : resolvedItems) {
            ProductOrderItem orderItem = new ProductOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(item.productId());
            orderItem.setSkuId(item.skuId());
            orderItem.setSkuCode(item.skuCode());
            orderItem.setSkuName(item.skuName());
            orderItem.setProductCode(item.productCode());
            orderItem.setProductName(item.productName());
            orderItem.setProductImage(item.productImage());
            orderItem.setUnitPrice(item.unitPrice());
            orderItem.setQuantity(item.quantity());
            orderItem.setSubtotal(item.subtotal());
            productOrderItemRepository.save(orderItem);
        }

        return toView(order);
    }

    @Transactional(readOnly = true)
    public ProductOrderView getOrder(String orderNo) {
        ProductOrder order = productOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return toView(order);
    }

    @Transactional(readOnly = true)
    public ProductOrderView getUserOrder(Long userId, String orderNo) {
        ProductOrder order = productOrderRepository.findByOrderNo(orderNo)
                .filter(item -> Objects.equals(item.getUserId(), userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return toView(order);
    }

    @Transactional(readOnly = true)
    public ProductOrderView getMerchantOrder(String merchantNo, String orderNo) {
        ProductOrder order = productOrderRepository.findByOrderNo(orderNo)
                .filter(item -> merchantNo.equals(item.getMerchantNo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return toView(order);
    }

    @Transactional(readOnly = true)
    public ProductOrderView getOrderById(Long id) {
        ProductOrder order = productOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return toView(order);
    }

    @Transactional(readOnly = true)
    public List<ProductOrderView> getUserOrders(Long userId) {
        return productOrderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductOrderView> searchOrders(String merchantNo, String orderNo, String status, String keyword,
                                               LocalDateTime createdAtStart, LocalDateTime createdAtEnd, int page, int size) {
        Specification<ProductOrder> spec = Specification.where(null);
        if (merchantNo != null && !merchantNo.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("merchantNo"), merchantNo));
        }
        if (orderNo != null && !orderNo.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(root.get("orderNo"), "%" + orderNo.trim() + "%"));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(root.get("orderNo"), "%" + keyword + "%"),
                    cb.like(root.get("userPhone"), "%" + keyword + "%"),
                    cb.like(root.get("merchantName"), "%" + keyword + "%")
            ));
        }
        if (createdAtStart != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart));
        }
        if (createdAtEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductOrder> result = productOrderRepository.findAll(spec, pageable);
        return result.map(this::toView);
    }

    @Transactional
    public void updateStatus(Long id, String newStatus) {
        ProductOrder order = productOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        order.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case "PAID" -> order.setPaidAt(now);
            case "SHIPPED" -> order.setShippedAt(now);
            case "DELIVERED" -> order.setDeliveredAt(now);
            case "COMPLETED" -> order.setCompletedAt(now);
            case "CANCELLED" -> order.setCancelledAt(now);
        }
        productOrderRepository.save(order);
    }

    @Transactional
    public ProductOrderView updateStatus(String orderNo, String newStatus) {
        ProductOrder order = productOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return updateStatus(order, newStatus);
    }

    @Transactional
    public ProductOrderView updateMerchantStatus(String merchantNo, String orderNo, String newStatus) {
        ProductOrder order = productOrderRepository.findByOrderNo(orderNo)
                .filter(item -> merchantNo.equals(item.getMerchantNo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        return updateStatus(order, newStatus);
    }

    private ProductOrderView updateStatus(ProductOrder order, String newStatus) {
        if (!List.of("PENDING", "PAID", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "REFUNDING", "REFUNDED").contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单状态不正确");
        }
        order.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case "PAID" -> order.setPaidAt(now);
            case "SHIPPED" -> order.setShippedAt(now);
            case "DELIVERED" -> order.setDeliveredAt(now);
            case "COMPLETED" -> order.setCompletedAt(now);
            case "CANCELLED" -> order.setCancelledAt(now);
        }
        productOrderRepository.save(order);
        return toView(order);
    }

    private ProductOrderView toView(ProductOrder order) {
        ProductOrderView view = ProductOrderView.from(order);
        // 加载明细
        List<ProductOrderItem> items = productOrderItemRepository.findByOrderId(order.getId());
        view.setItems(items.stream().map(ProductOrderItemView::from).toList());
        // 加载关联支付单
        var payments = paymentOrderRepository.findByProductOrderId(order.getId());
        view.setPayments(payments.stream().map(PaymentOrderView::from).toList());
        return view;
    }

    private String generateOrderNo() {
        long timestamp = System.currentTimeMillis();
        int rand = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "PO" + timestamp + rand;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }

    private String requiredShipping(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (value instanceof String s) {
            try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
        }
        return BigDecimal.ZERO;
    }

    private List<ResolvedOrderItem> resolveItems(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择商品");
        }
        List<ResolvedOrderItem> resolvedItems = new ArrayList<>();
        String merchantNo = null;
        for (Map<String, Object> item : items) {
            Long productId = toLong(item.get("productId"));
            Long skuId = item.get("skuId") == null ? null : toLong(item.get("skuId"));
            int quantity = toInt(item.get("quantity"), 1);
            if (productId == null || productId <= 0 || quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品数据不正确");
            }
            Map<String, Object> product = findOnSaleProduct(productId, skuId);
            String productMerchantNo = String.valueOf(product.get("merchant_no"));
            if (merchantNo == null) {
                merchantNo = productMerchantNo;
            } else if (!merchantNo.equals(productMerchantNo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先结算同一商家的商品");
            }
            int stock = toInt(product.get("sku_stock"), 0);
            if (stock < quantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品库存不足：" + product.get("product_name") + " " + product.get("sku_name"));
            }
            BigDecimal unitPrice = toBigDecimal(product.get("sku_price")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            resolvedItems.add(new ResolvedOrderItem(
                    productId,
                    toLong(product.get("sku_id")),
                    productMerchantNo,
                    String.valueOf(product.get("app_name")),
                    value(product.get("product_code")),
                    value(product.get("sku_code")),
                    value(product.get("sku_name")),
                    value(product.get("product_name")),
                    value(product.get("cover_image")),
                    unitPrice,
                    quantity,
                    subtotal
            ));
        }
        return resolvedItems;
    }

    private Map<String, Object> findOnSaleProduct(Long productId, Long skuId) {
        Object[] args = skuId == null
                ? new Object[] { productId }
                : new Object[] { productId, skuId };
        List<Map<String, Object>> rows = gatewayJdbcTemplate.queryForList(
                (skuId == null ? """
                        SELECT p.id, p.merchant_no, p.product_code, p.product_name,
                               s.id AS sku_id, s.sku_code, s.sku_name, s.price AS sku_price, s.stock AS sku_stock,
                               p.cover_image, a.app_name
                        FROM merchant_product p
                        JOIN gateway_app a ON a.app_id = p.merchant_no AND a.enabled = true
                        JOIN merchant_product_sku s ON s.product_id = p.id AND s.enabled = true
                        WHERE p.id = ? AND p.status = 'ON_SALE'
                        ORDER BY s.sort_order ASC, s.id ASC
                        LIMIT 1
                        """ : """
                        SELECT p.id, p.merchant_no, p.product_code, p.product_name,
                               s.id AS sku_id, s.sku_code, s.sku_name, s.price AS sku_price, s.stock AS sku_stock,
                               p.cover_image, a.app_name
                        FROM merchant_product p
                        JOIN gateway_app a ON a.app_id = p.merchant_no AND a.enabled = true
                        JOIN merchant_product_sku s ON s.product_id = p.id AND s.enabled = true
                        WHERE p.id = ? AND s.id = ? AND p.status = 'ON_SALE'
                        """),
                args
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品或SKU不存在、已下架");
        }
        return rows.get(0);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ResolvedOrderItem(
            Long productId,
            Long skuId,
            String merchantNo,
            String merchantName,
            String productCode,
            String skuCode,
            String skuName,
            String productName,
            String productImage,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {}
}
