package com.example.mallhome.domain;

import com.example.mallhome.entity.ProductOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductOrderView {

    private Long id;
    private String orderNo;
    private Long userId;
    private String userPhone;
    private String userDisplayName;
    private String merchantNo;
    private String merchantName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private String status;
    private String shippingAddress;
    private String shippingName;
    private String shippingPhone;
    private String remark;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductOrderItemView> items;
    private List<PaymentOrderView> payments;

    public static ProductOrderView from(ProductOrder order) {
        ProductOrderView view = new ProductOrderView();
        view.setId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setUserId(order.getUserId());
        view.setUserPhone(order.getUserPhone());
        view.setUserDisplayName(order.getUserDisplayName());
        view.setMerchantNo(order.getMerchantNo());
        view.setMerchantName(order.getMerchantName());
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setShippingFee(order.getShippingFee());
        view.setStatus(order.getStatus());
        view.setShippingAddress(order.getShippingAddress());
        view.setShippingName(order.getShippingName());
        view.setShippingPhone(order.getShippingPhone());
        view.setRemark(order.getRemark());
        view.setPaidAt(order.getPaidAt());
        view.setShippedAt(order.getShippedAt());
        view.setDeliveredAt(order.getDeliveredAt());
        view.setCompletedAt(order.getCompletedAt());
        view.setCancelledAt(order.getCancelledAt());
        view.setCreatedAt(order.getCreatedAt());
        view.setUpdatedAt(order.getUpdatedAt());
        return view;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public String getMerchantNo() { return merchantNo; }
    public void setMerchantNo(String merchantNo) { this.merchantNo = merchantNo; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }
    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ProductOrderItemView> getItems() { return items; }
    public void setItems(List<ProductOrderItemView> items) { this.items = items; }
    public List<PaymentOrderView> getPayments() { return payments; }
    public void setPayments(List<PaymentOrderView> payments) { this.payments = payments; }
}
