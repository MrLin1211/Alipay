package com.example.mallhome.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mallhome.pay")
public class MallhomePayProperties {

    @NotBlank
    private String host;

    @NotBlank
    private String md5Key;

    @NotBlank
    private String aesKey;

    @NotBlank
    private String externalId;

    @NotBlank
    private String notifyUrl;

    private String returnUrl;

    private String defaultPayMethodType = "ALIPAY_CN";

    private String gatewayHost = "http://127.0.0.1:8090";

    private String gatewayAppId = "biz-demo";

    private String gatewayAppSecret = "demo-secret-change-me";

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = trimTrailingSlash(host);
    }

    public String getMd5Key() {
        return md5Key;
    }

    public void setMd5Key(String md5Key) {
        this.md5Key = md5Key;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
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

    public String getDefaultPayMethodType() {
        return defaultPayMethodType;
    }

    public void setDefaultPayMethodType(String defaultPayMethodType) {
        this.defaultPayMethodType = defaultPayMethodType;
    }

    public String getGatewayHost() {
        return gatewayHost;
    }

    public void setGatewayHost(String gatewayHost) {
        this.gatewayHost = trimTrailingSlash(gatewayHost);
    }

    public String getGatewayAppId() {
        return gatewayAppId;
    }

    public void setGatewayAppId(String gatewayAppId) {
        this.gatewayAppId = gatewayAppId;
    }

    public String getGatewayAppSecret() {
        return gatewayAppSecret;
    }

    public void setGatewayAppSecret(String gatewayAppSecret) {
        this.gatewayAppSecret = gatewayAppSecret;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
