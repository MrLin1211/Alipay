package com.example.mallhome.config;

import com.example.mallhome.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AdminAuthService authService;

    public AdminAuthFilter(AdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/admin/")) {
            return true;
        }
        return "/api/admin/auth/login".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            authService.requireUser(extractBearerToken(request.getHeader("Authorization")));
            filterChain.doFilter(request, response);
        } catch (ResponseStatusException exception) {
            response.setStatus(exception.getStatusCode().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + exception.getStatusCode().value()
                    + ",\"message\":\"" + exception.getReason() + "\"}");
        } catch (Exception exception) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
        }
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }
}
