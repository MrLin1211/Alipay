package com.example.mallhome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_refund",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_refund_request_no", columnNames = "request_no")
        }
)
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_no", nullable = false, length = 64)
    private String requestNo;

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "plat_trade_no", nullable = false, length = 128)
    private String platTradeNo;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", nullable = false, length = 256)
    private String refundReason;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "trade_status", length = 64)
    private String tradeStatus;

    @Lob
    @Column(name = "platform_response")
    private String platformResponse;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaymentRefund() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPlatTradeNo() { return platTradeNo; }
    public void setPlatTradeNo(String platTradeNo) { this.platTradeNo = platTradeNo; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTradeStatus() { return tradeStatus; }
    public void setTradeStatus(String tradeStatus) { this.tradeStatus = tradeStatus; }
    public String getPlatformResponse() { return platformResponse; }
    public void setPlatformResponse(String platformResponse) { this.platformResponse = platformResponse; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
