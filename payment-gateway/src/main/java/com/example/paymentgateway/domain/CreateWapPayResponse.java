package com.example.paymentgateway.domain;

public class CreateWapPayResponse {
    private final String gatewayOrderNo;
    private final String merchantOrderNo;
    private final String payForm;

    public CreateWapPayResponse(String gatewayOrderNo, String merchantOrderNo, String payForm) {
        this.gatewayOrderNo = gatewayOrderNo;
        this.merchantOrderNo = merchantOrderNo;
        this.payForm = payForm;
    }

    public String getGatewayOrderNo() { return gatewayOrderNo; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public String getPayForm() { return payForm; }
}
