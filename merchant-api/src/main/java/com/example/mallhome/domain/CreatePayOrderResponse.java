package com.example.mallhome.domain;

public class CreatePayOrderResponse {

    private Long orderId;
    private String orderNo;
    private String status;
    private String payUrl;
    private String platTradeNo;
    private String rawResponse;

    public CreatePayOrderResponse(Long orderId, String orderNo, String status, String payUrl, String platTradeNo, String rawResponse) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.status = status;
        this.payUrl = payUrl;
        this.platTradeNo = platTradeNo;
        this.rawResponse = rawResponse;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public String getPlatTradeNo() {
        return platTradeNo;
    }

    public void setPlatTradeNo(String platTradeNo) {
        this.platTradeNo = platTradeNo;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
