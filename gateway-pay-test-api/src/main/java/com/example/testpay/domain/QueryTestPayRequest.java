package com.example.testpay.domain;

public class QueryTestPayRequest {

    private String gatewayOrderNo;
    private String merchantOrderNo;

    public String getGatewayOrderNo() { return gatewayOrderNo; }
    public void setGatewayOrderNo(String gatewayOrderNo) { this.gatewayOrderNo = gatewayOrderNo; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
}
