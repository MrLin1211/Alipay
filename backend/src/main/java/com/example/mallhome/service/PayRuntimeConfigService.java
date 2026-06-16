package com.example.mallhome.service;

import com.example.mallhome.domain.PayChannel;
import com.example.mallhome.domain.PayRuntimeConfig;
import com.example.mallhome.domain.UpdatePayConfigRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Service
public class PayRuntimeConfigService {

    private final JdbcTemplate jdbcTemplate;

    public PayRuntimeConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PayRuntimeConfig getConfig() {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT * FROM pay_runtime_config ORDER BY id LIMIT 1
                """);
        PayRuntimeConfig config = new PayRuntimeConfig();
        config.setPayChannel(string(row, "pay_channel"));
        config.setHost(string(row, "mallhome_host"));
        config.setExternalId(string(row, "external_id"));
        config.setNotifyUrl(string(row, "notify_url"));
        config.setReturnUrl(string(row, "return_url"));
        config.setDefaultPayMethodType(string(row, "default_pay_method_type"));
        config.setGatewayHost(string(row, "gateway_host"));
        config.setGatewayAppId(string(row, "gateway_app_id"));
        config.setGatewayAppSecret(string(row, "gateway_app_secret"));
        config.setGatewayReturnUrl(string(row, "gateway_return_url"));
        config.setGatewayBusinessNotifyUrl(string(row, "gateway_business_notify_url"));
        return config;
    }

    public PayRuntimeConfig getMaskedConfig() {
        PayRuntimeConfig config = getConfig();
        config.setGatewayAppSecret(mask(config.getGatewayAppSecret()));
        return config;
    }

    public void updateConfig(UpdatePayConfigRequest request) {
        PayRuntimeConfig current = getConfig();
        String payChannel = value(request.getPayChannel(), current.getPayChannel());
        if (!PayChannel.MALLHOME.equals(payChannel) && !PayChannel.PAYMENT_GATEWAY.equals(payChannel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付通道不合法");
        }

        jdbcTemplate.update(
                """
                        UPDATE pay_runtime_config
                        SET pay_channel = ?, mallhome_host = ?, external_id = ?, notify_url = ?,
                            return_url = ?, default_pay_method_type = ?, gateway_host = ?,
                            gateway_app_id = ?, gateway_app_secret = ?, gateway_return_url = ?,
                            gateway_business_notify_url = ?, updated_at = NOW()
                        ORDER BY id LIMIT 1
                        """,
                payChannel,
                trimTrailingSlash(value(request.getHost(), current.getHost())),
                value(request.getExternalId(), current.getExternalId()),
                value(request.getNotifyUrl(), current.getNotifyUrl()),
                value(request.getReturnUrl(), current.getReturnUrl()),
                value(request.getDefaultPayMethodType(), current.getDefaultPayMethodType()),
                trimTrailingSlash(value(request.getGatewayHost(), current.getGatewayHost())),
                value(request.getGatewayAppId(), current.getGatewayAppId()),
                secretValue(request.getGatewayAppSecret(), current.getGatewayAppSecret()),
                value(request.getGatewayReturnUrl(), current.getGatewayReturnUrl()),
                value(request.getGatewayBusinessNotifyUrl(), current.getGatewayBusinessNotifyUrl())
        );
    }

    private static String string(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String value(String incoming, String fallback) {
        return incoming == null ? fallback : incoming.trim();
    }

    private static String secretValue(String incoming, String fallback) {
        if (!StringUtils.hasText(incoming) || incoming.contains("*")) {
            return fallback;
        }
        return incoming.trim();
    }

    private static String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 8) {
            return "********";
        }
        return value.substring(0, 4) + "********" + value.substring(value.length() - 4);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
