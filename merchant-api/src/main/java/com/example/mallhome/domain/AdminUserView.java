package com.example.mallhome.domain;

import com.example.mallhome.entity.AdminUser;

import java.time.LocalDateTime;

public class AdminUserView {

    private Long id;
    private String username;
    private String displayName;
    private Boolean enabled;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserView from(AdminUser user) {
        AdminUserView view = new AdminUserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setDisplayName(user.getDisplayName());
        view.setEnabled(user.getEnabled());
        view.setLastLoginAt(user.getLastLoginAt());
        view.setCreatedAt(user.getCreatedAt());
        view.setUpdatedAt(user.getUpdatedAt());
        return view;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
