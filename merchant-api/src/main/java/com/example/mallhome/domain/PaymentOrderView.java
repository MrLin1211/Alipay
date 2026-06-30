package com.example.mallhome.domain;

import com.example.mallhome.entity.PaymentOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentOrderView {

    private Long id;
    private String orderNo;
    private String externalId;
    private Long customerUserId;
    private String customerDisplayName;
    private BigDecimal totalAmount;
    private String subject;
    private String clientIp;
    private String status;
    private String tradeStatus;
    private String platTradeNo;
    private String thirdOutTradeNo;
    private String payUrl;
    private String evokeMode;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentOrderView from(PaymentOrder order) {
        PaymentOrderView view = new PaymentOrderView();
        view.setId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setExternalId(order.getExternalId());
        view.setCustomerUserId(order.getCustomerUserId());
        view.setCustomerDisplayName(order.getCustomerDisplayName());
        view.setTotalAmount(order.getTotalAmount());
        view.setSubject(order.getSubject());
        view.setClientIp(order.getClientIp());
        view.setStatus(order.getStatus());
        view.setTradeStatus(order.getTradeStatus());
        view.setPlatTradeNo(order.getPlatTradeNo());
        view.setThirdOutTradeNo(order.getThirdOutTradeNo());
        view.setPayUrl(order.getPayUrl());
        view.setEvokeMode(order.getEvokeMode());
        view.setPaidAt(order.getPaidAt());
        view.setCreatedAt(order.getCreatedAt());
        view.setUpdatedAt(order.getUpdatedAt());
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
