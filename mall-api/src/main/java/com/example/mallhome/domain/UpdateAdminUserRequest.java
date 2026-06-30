package com.example.mallhome.domain;

import jakarta.validation.constraints.Size;

public class UpdateAdminUserRequest {

    @Size(max = 64, message = "显示名称长度不能超过64个字符")
    private String displayName;

    @Size(min = 6, max = 128, message = "密码长度必须在6到128位之间")
    private String password;

    private Boolean enabled;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
