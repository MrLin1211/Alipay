package com.example.mallhome.domain;

import com.example.mallhome.entity.PaymentRefund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentRefundView {

    private Long id;
    private String requestNo;
    private String orderNo;
    private String platTradeNo;
    private BigDecimal refundAmount;
    private String refundReason;
    private String status;
    private String tradeStatus;
    private String platformResponse;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentRefundView from(PaymentRefund refund) {
        PaymentRefundView view = new PaymentRefundView();
        view.id = refund.getId();
        view.requestNo = refund.getRequestNo();
        view.orderNo = refund.getOrderNo();
        view.platTradeNo = refund.getPlatTradeNo();
        view.refundAmount = refund.getRefundAmount();
        view.refundReason = refund.getRefundReason();
        view.status = refund.getStatus();
        view.tradeStatus = refund.getTradeStatus();
        view.platformResponse = refund.getPlatformResponse();
        view.createdBy = refund.getCreatedBy();
        view.createdAt = refund.getCreatedAt();
        view.updatedAt = refund.getUpdatedAt();
        return view;
    }

    public Long getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public String getOrderNo() { return orderNo; }
    public String getPlatTradeNo() { return platTradeNo; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public String getRefundReason() { return refundReason; }
    public String getStatus() { return status; }
    public String getTradeStatus() { return tradeStatus; }
    public String getPlatformResponse() { return platformResponse; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
