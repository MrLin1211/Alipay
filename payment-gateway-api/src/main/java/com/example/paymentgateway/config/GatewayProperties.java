package com.example.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private final Admin admin = new Admin();
    private final DemoApp demoApp = new DemoApp();

    public Admin getAdmin() {
        return admin;
    }

    public DemoApp getDemoApp() {
        return demoApp;
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
}
