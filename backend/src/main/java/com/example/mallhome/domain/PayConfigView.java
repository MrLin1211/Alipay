package com.example.mallhome.domain;

public class PayConfigView {

    private String host;
    private String externalId;
    private String notifyUrl;
    private String returnUrl;

    public PayConfigView(String host, String externalId, String notifyUrl, String returnUrl) {
        this.host = host;
        this.externalId = externalId;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
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
}
