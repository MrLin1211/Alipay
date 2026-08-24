package com.example.paymentgateway.domain;

import jakarta.validation.constraints.Size;

public class TradeQueryRequest {

    @Size(max = 64, message = "网关订单号不能超过64个字符")
    private String gatewayOrderNo;

    @Size(max = 64, message = "业务订单号不能超过64个字符")
    private String merchantOrderNo;

    public String getGatewayOrderNo() { return gatewayOrderNo; }
    public void setGatewayOrderNo(String gatewayOrderNo) { this.gatewayOrderNo = gatewayOrderNo; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
}
