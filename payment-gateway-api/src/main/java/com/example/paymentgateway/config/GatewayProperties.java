package com.example.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private final Admin admin = new Admin();
    private final Client client = new Client();
    private final DemoApp demoApp = new DemoApp();
    private final Zhenbaoge zhenbaoge = new Zhenbaoge();

    public Admin getAdmin() {
        return admin;
    }

    public DemoApp getDemoApp() {
        return demoApp;
    }

    public Client getClient() {
        return client;
    }

    public Zhenbaoge getZhenbaoge() {
        return zhenbaoge;
    }

    public static class Admin {
        private String defaultUsername = "admin";
        private String defaultPassword = "admin123456";

        public String getDefaultUsername() {
            return defaultUsername;
        }

        public void setDefaultUsername(String defaultUsername) {
            this.defaultUsername = defaultUsername;
        }

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }
    }

    public static class Client {
        private String defaultUsername = "client";
        private String defaultPassword = "client123456";
        private String defaultName = "演示接入方";

        public String getDefaultUsername() {
            return defaultUsername;
        }

        public void setDefaultUsername(String defaultUsername) {
            this.defaultUsername = defaultUsername;
        }

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }

        public String getDefaultName() {
            return defaultName;
        }

        public void setDefaultName(String defaultName) {
            this.defaultName = defaultName;
        }
    }

    public static class DemoApp {
        private boolean enabled = true;
        private String appId = "biz-demo";
        private String appSecret = "demo-secret-change-me";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }
    }

    public static class Zhenbaoge {
        private String host = "https://pay.zhenbaoge.com";
        private String externalId = "";
        private String md5Key = "";
        private String aesKey = "";
        private String notifyUrl = "";
        private boolean enabled = true;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getMd5Key() { return md5Key; }
        public void setMd5Key(String md5Key) { this.md5Key = md5Key; }
        public String getAesKey() { return aesKey; }
        public void setAesKey(String aesKey) { this.aesKey = aesKey; }
        public String getNotifyUrl() { return notifyUrl; }
        public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
