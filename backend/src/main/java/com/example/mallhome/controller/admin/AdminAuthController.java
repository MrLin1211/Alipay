package com.example.mallhome.controller.admin;

import com.example.mallhome.domain.AdminLoginRequest;
import com.example.mallhome.domain.AdminLoginResponse;
import com.example.mallhome.domain.AdminUserView;
import com.example.mallhome.domain.CreateAdminUserRequest;
import com.example.mallhome.domain.UpdateAdminUserRequest;
import com.example.mallhome.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractBearerToken(authorization));
    }

    @GetMapping("/auth/me")
    public AdminUserView me(@RequestHeader("Authorization") String authorization) {
        return AdminUserView.from(authService.requireUser(extractBearerToken(authorization)));
    }

    @GetMapping("/users")
    public List<AdminUserView> listUsers() {
        return authService.listUsers();
    }

    @PostMapping("/users")
    public AdminUserView createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return authService.createUser(request);
    }

    @PatchMapping("/users/{id}")
    public AdminUserView updateUser(@PathVariable Long id, @Valid @RequestBody UpdateAdminUserRequest request) {
        return authService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }
}
