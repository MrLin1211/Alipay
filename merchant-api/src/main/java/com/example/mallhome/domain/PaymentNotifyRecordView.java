package com.example.mallhome.domain;

import com.example.mallhome.entity.PaymentNotifyRecord;

import java.time.LocalDateTime;

public class PaymentNotifyRecordView {

    private Long id;
    private String orderNo;
    private String externalId;
    private String tradeStatus;
    private String platformOutTradeNo;
    private Boolean verified;
    private String result;
    private String failureReason;
    private String notifyPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentNotifyRecordView from(PaymentNotifyRecord record) {
        PaymentNotifyRecordView view = new PaymentNotifyRecordView();
        view.setId(record.getId());
        view.setOrderNo(record.getOrderNo());
        view.setExternalId(record.getExternalId());
        view.setTradeStatus(record.getTradeStatus());
        view.setPlatformOutTradeNo(record.getPlatformOutTradeNo());
        view.setVerified(record.getVerified());
        view.setResult(record.getResult());
        view.setFailureReason(record.getFailureReason());
        view.setNotifyPayload(record.getNotifyPayload());
        view.setCreatedAt(record.getCreatedAt());
        view.setUpdatedAt(record.getUpdatedAt());
        return view;
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
