package com.example.mallhome.domain;

public class PayConfigView {

    private String host;
    private String externalId;
    private String notifyUrl;
    private String returnUrl;
    private String payChannel;
    private String defaultPayMethodType;
    private String gatewayHost;
    private String gatewayAppId;
    private String gatewayAppSecret;
    private String gatewayReturnUrl;
    private String gatewayBusinessNotifyUrl;

    public PayConfigView(
            String payChannel,
            String host,
            String externalId,
            String notifyUrl,
            String returnUrl,
            String defaultPayMethodType,
            String gatewayHost,
            String gatewayAppId,
            String gatewayAppSecret,
            String gatewayReturnUrl,
            String gatewayBusinessNotifyUrl
    ) {
        this.payChannel = payChannel;
        this.host = host;
        this.externalId = externalId;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        this.defaultPayMethodType = defaultPayMethodType;
        this.gatewayHost = gatewayHost;
        this.gatewayAppId = gatewayAppId;
        this.gatewayAppSecret = gatewayAppSecret;
        this.gatewayReturnUrl = gatewayReturnUrl;
        this.gatewayBusinessNotifyUrl = gatewayBusinessNotifyUrl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getPayChannel() { return payChannel; }
    public void setPayChannel(String payChannel) { this.payChannel = payChannel; }
    public String getDefaultPayMethodType() { return defaultPayMethodType; }
    public void setDefaultPayMethodType(String defaultPayMethodType) { this.defaultPayMethodType = defaultPayMethodType; }
    public String getGatewayHost() { return gatewayHost; }
    public void setGatewayHost(String gatewayHost) { this.gatewayHost = gatewayHost; }
    public String getGatewayAppId() { return gatewayAppId; }
    public void setGatewayAppId(String gatewayAppId) { this.gatewayAppId = gatewayAppId; }
    public String getGatewayAppSecret() { return gatewayAppSecret; }
    public void setGatewayAppSecret(String gatewayAppSecret) { this.gatewayAppSecret = gatewayAppSecret; }
    public String getGatewayReturnUrl() { return gatewayReturnUrl; }
    public void setGatewayReturnUrl(String gatewayReturnUrl) { this.gatewayReturnUrl = gatewayReturnUrl; }
    public String getGatewayBusinessNotifyUrl() { return gatewayBusinessNotifyUrl; }
    public void setGatewayBusinessNotifyUrl(String gatewayBusinessNotifyUrl) { this.gatewayBusinessNotifyUrl = gatewayBusinessNotifyUrl; }
}
