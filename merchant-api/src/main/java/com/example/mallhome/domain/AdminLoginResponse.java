package com.example.mallhome.domain;

import java.time.LocalDateTime;

public class AdminLoginResponse {

    private String token;
    private LocalDateTime expiresAt;
    private AdminUserView user;

    public AdminLoginResponse(String token, LocalDateTime expiresAt, AdminUserView user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public AdminUserView getUser() {
        return user;
    }

    public void setUser(AdminUserView user) {
        this.user = user;
    }
}
