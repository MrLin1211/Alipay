package com.example.mallhome.domain;

import com.example.mallhome.entity.ProductOrderItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductOrderItemView {

    private Long id;
    private Long orderId;
    private Long productId;
    private Long skuId;
    private String productCode;
    private String skuCode;
    private String skuName;
    private String productName;
    private String productImage;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;

    public static ProductOrderItemView from(ProductOrderItem item) {
        ProductOrderItemView view = new ProductOrderItemView();
        view.setId(item.getId());
        view.setOrderId(item.getOrderId());
        view.setProductId(item.getProductId());
        view.setSkuId(item.getSkuId());
        view.setProductCode(item.getProductCode());
        view.setSkuCode(item.getSkuCode());
        view.setSkuName(item.getSkuName());
        view.setProductName(item.getProductName());
        view.setProductImage(item.getProductImage());
        view.setUnitPrice(item.getUnitPrice());
        view.setQuantity(item.getQuantity());
        view.setSubtotal(item.getSubtotal());
        view.setCreatedAt(item.getCreatedAt());
        return view;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
