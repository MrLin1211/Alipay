package com.example.testpay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "testpay")
public class TestPayProperties {

    private String gatewayBaseUrl;
    private String appId;
    private String appSecret;
    private String returnUrl;
    private String businessNotifyUrl;

    public String getGatewayBaseUrl() { return gatewayBaseUrl; }
    public void setGatewayBaseUrl(String gatewayBaseUrl) { this.gatewayBaseUrl = gatewayBaseUrl; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getBusinessNotifyUrl() { return businessNotifyUrl; }
    public void setBusinessNotifyUrl(String businessNotifyUrl) { this.businessNotifyUrl = businessNotifyUrl; }
}
