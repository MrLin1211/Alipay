package com.example.mallhome.domain;

import com.example.mallhome.entity.PaymentOrder;

public class PaymentOrderDetailView extends PaymentOrderView {

    private String platformCreateResponse;
    private String notifyPayload;

    public static PaymentOrderDetailView fromOrder(PaymentOrder order) {
        PaymentOrderDetailView view = new PaymentOrderDetailView();
        PaymentOrderView base = PaymentOrderView.from(order);
        view.setId(base.getId());
        view.setOrderNo(base.getOrderNo());
        view.setExternalId(base.getExternalId());
        view.setTotalAmount(base.getTotalAmount());
        view.setSubject(base.getSubject());
        view.setClientIp(base.getClientIp());
        view.setStatus(base.getStatus());
        view.setTradeStatus(base.getTradeStatus());
        view.setPlatTradeNo(base.getPlatTradeNo());
        view.setThirdOutTradeNo(base.getThirdOutTradeNo());
        view.setPayUrl(base.getPayUrl());
        view.setEvokeMode(base.getEvokeMode());
        view.setPaidAt(base.getPaidAt());
        view.setCreatedAt(base.getCreatedAt());
        view.setUpdatedAt(base.getUpdatedAt());
        view.setPlatformCreateResponse(order.getPlatformCreateResponse());
        view.setNotifyPayload(order.getNotifyPayload());
        return view;
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
}
