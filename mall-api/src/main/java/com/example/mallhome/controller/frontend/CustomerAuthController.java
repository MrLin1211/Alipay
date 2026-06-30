package com.example.mallhome.controller.frontend;

import com.example.mallhome.service.CustomerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mall/customer/auth")
public class CustomerAuthController {

    private final CustomerAuthService authService;

    public CustomerAuthController(CustomerAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> payload) {
        return authService.register(payload);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> payload) {
        return authService.login(payload);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        authService.logout(extractToken(request));
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return authService.userView(authService.requireUser(extractToken(request)));
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return "";
    }
}
