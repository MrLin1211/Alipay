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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_notify_record",
        indexes = {
                @Index(name = "idx_payment_notify_order_no", columnList = "order_no"),
                @Index(name = "idx_payment_notify_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_notify_key", columnNames = "notify_key")
        }
)
public class PaymentNotifyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "trade_status", length = 64)
    private String tradeStatus;

    @Column(name = "platform_out_trade_no", length = 128)
    private String platformOutTradeNo;

    @Column(name = "notify_key", nullable = false, length = 255)
    private String notifyKey;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "result", nullable = false, length = 32)
    private String result;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Lob
    @Column(name = "notify_payload")
    private String notifyPayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaymentNotifyRecord() {
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

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(String tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public String getPlatformOutTradeNo() {
        return platformOutTradeNo;
    }

    public void setPlatformOutTradeNo(String platformOutTradeNo) {
        this.platformOutTradeNo = platformOutTradeNo;
    }

    public String getNotifyKey() {
        return notifyKey;
    }

    public void setNotifyKey(String notifyKey) {
        this.notifyKey = notifyKey;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getNotifyPayload() {
        return notifyPayload;
    }

    public void setNotifyPayload(String notifyPayload) {
        this.notifyPayload = notifyPayload;
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
