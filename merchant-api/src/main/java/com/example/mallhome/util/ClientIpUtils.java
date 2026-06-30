package com.example.mallhome.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;

public final class ClientIpUtils {

    // 如果本地开发或代理环境无法拿到真实公网 IP，平台下单参数使用这个固定值兜底。
    public static final String FALLBACK_CLIENT_IP = "182.144.176.124";

    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    private ClientIpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                return normalize(value.split(",")[0].trim());
            }
        }
        return normalize(request.getRemoteAddr());
    }

    private static String normalize(String ip) {
        if (!StringUtils.hasText(ip) || isLoopback(ip)) {
            return FALLBACK_CLIENT_IP;
        }
        return ip;
    }

    private static boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip)
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "localhost".equalsIgnoreCase(ip);
    }
}
