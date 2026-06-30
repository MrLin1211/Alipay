package com.example.mallhome.config;

import com.example.mallhome.service.MerchantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Component
public class MerchantAuthFilter extends OncePerRequestFilter {

    private final MerchantService merchantService;

    public MerchantAuthFilter(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/merchant/")) {
            return true;
        }
        return "/api/merchant/auth/login".equals(path)
                || "/api/merchant/auth/register".equals(path)
                || "/api/merchant/gateway/pay/notify".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            merchantService.requireMerchant(extractBearerToken(request.getHeader("Authorization")));
            filterChain.doFilter(request, response);
        } catch (ResponseStatusException exception) {
            applyCorsHeaders(request, response);
            response.setStatus(exception.getStatusCode().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + exception.getStatusCode().value()
                    + ",\"message\":\"" + exception.getReason() + "\"}");
        } catch (Exception exception) {
            applyCorsHeaders(request, response);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
        }
    }

    private static void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }
}
