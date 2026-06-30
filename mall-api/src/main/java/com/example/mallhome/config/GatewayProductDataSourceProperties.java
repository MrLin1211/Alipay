package com.example.mallhome.config;

public class GatewayProductDataSourceProperties {

    private String url = "jdbc:mysql://127.0.0.1:3306/payment_gateway?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";

    private String username = "alipay_user";

    private String password = "";

    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }
}
