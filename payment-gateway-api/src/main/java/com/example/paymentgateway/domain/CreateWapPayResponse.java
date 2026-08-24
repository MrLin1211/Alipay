package com.example.paymentgateway.domain;

public class CreateWapPayResponse {
    private final String gatewayOrderNo;
    private final String merchantOrderNo;
    private final String payForm;
    private final String payUrl;
    private final String evokeMode;
    private final String platformTradeNo;

    public CreateWapPayResponse(String gatewayOrderNo, String merchantOrderNo, String payForm) {
        this(gatewayOrderNo, merchantOrderNo, payForm, null, null, null);
    }

    public CreateWapPayResponse(String gatewayOrderNo, String merchantOrderNo, String payForm,
                                String payUrl, String evokeMode, String platformTradeNo) {
        this.gatewayOrderNo = gatewayOrderNo;
        this.merchantOrderNo = merchantOrderNo;
        this.payForm = payForm;
        this.payUrl = payUrl;
        this.evokeMode = evokeMode;
        this.platformTradeNo = platformTradeNo;
    }

    public String getGatewayOrderNo() { return gatewayOrderNo; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public String getPayForm() { return payForm; }
    public String getPayUrl() { return payUrl; }
    public String getEvokeMode() { return evokeMode; }
    public String getPlatformTradeNo() { return platformTradeNo; }
}
