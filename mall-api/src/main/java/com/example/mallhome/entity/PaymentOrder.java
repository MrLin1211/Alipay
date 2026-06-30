package com.example.mallhome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_order",
        indexes = {
                @Index(name = "idx_payment_order_order_no", columnList = "order_no", unique = true),
                @Index(name = "idx_payment_order_plat_trade_no", columnList = "plat_trade_no")
        }
)
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "product_order_id")
    private Long productOrderId;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "customer_user_id")
    private Long customerUserId;

    @Column(name = "customer_display_name", length = 64)
    private String customerDisplayName;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    @Column(name = "client_ip", nullable = false, length = 64)
    private String clientIp;

    @Column(name = "type_index")
    private Integer typeIndex;

    @Column(name = "goods_type")
    private Integer goodsType;

    @Column(name = "pay_method_type", length = 32)
    private String payMethodType;

    @Column(name = "attach_info", length = 512)
    private String attachInfo;

    @Column(name = "return_url", length = 512)
    private String returnUrl;

    @Column(name = "quit_url", length = 512)
    private String quitUrl;

    @Column(name = "sub_external_id", length = 64)
    private String subExternalId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "trade_status", length = 64)
    private String tradeStatus;

    @Column(name = "plat_trade_no", length = 128)
    private String platTradeNo;

    @Column(name = "third_out_trade_no", length = 128)
    private String thirdOutTradeNo;

    @Column(name = "pay_url", length = 2048)
    private String payUrl;

    @Column(name = "evoke_mode", length = 16)
    private String evokeMode;

    @Lob
    @Column(name = "platform_create_response")
    private String platformCreateResponse;

    @Lob
    @Column(name = "notify_payload")
    private String notifyPayload;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaymentOrder() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getProductOrderId() {
        return productOrderId;
    }

    public void setProductOrderId(Long productOrderId) {
        this.productOrderId = productOrderId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public void setCustomerUserId(Long customerUserId) {
        this.customerUserId = customerUserId;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public void setCustomerDisplayName(String customerDisplayName) {
        this.customerDisplayName = customerDisplayName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getTypeIndex() {
        return typeIndex;
    }

    public void setTypeIndex(Integer typeIndex) {
        this.typeIndex = typeIndex;
    }

    public Integer getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(Integer goodsType) {
        this.goodsType = goodsType;
    }

    public String getPayMethodType() {
        return payMethodType;
    }

    public void setPayMethodType(String payMethodType) {
        this.payMethodType = payMethodType;
    }

    public String getAttachInfo() {
        return attachInfo;
    }

    public void setAttachInfo(String attachInfo) {
        this.attachInfo = attachInfo;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getQuitUrl() {
        return quitUrl;
    }

    public void setQuitUrl(String quitUrl) {
        this.quitUrl = quitUrl;
    }

    public String getSubExternalId() {
        return subExternalId;
    }

    public void setSubExternalId(String subExternalId) {
        this.subExternalId = subExternalId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(String tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public String getPlatTradeNo() {
        return platTradeNo;
    }

    public void setPlatTradeNo(String platTradeNo) {
        this.platTradeNo = platTradeNo;
    }

    public String getThirdOutTradeNo() {
        return thirdOutTradeNo;
    }

    public void setThirdOutTradeNo(String thirdOutTradeNo) {
        this.thirdOutTradeNo = thirdOutTradeNo;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public String getEvokeMode() {
        return evokeMode;
    }

    public void setEvokeMode(String evokeMode) {
        this.evokeMode = evokeMode;
    }

    public String getPlatformCreateResponse() {
        return platformCreateResponse;
    }

    public void setPlatformCreateResponse(String platformCreateResponse) {
        this.platformCreateResponse = platformCreateResponse;
    }

    public String getNotifyPayload() {
        return notifyPayload;
    }

    public void setNotifyPayload(String notifyPayload) {
        this.notifyPayload = notifyPayload;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
