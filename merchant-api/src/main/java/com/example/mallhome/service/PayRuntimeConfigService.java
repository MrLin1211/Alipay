package com.example.mallhome.service;

import com.example.mallhome.domain.PayChannel;
import com.example.mallhome.domain.PayRuntimeConfig;
import com.example.mallhome.domain.UpdatePayConfigRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class PayRuntimeConfigService {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate gatewayJdbcTemplate;

    public PayRuntimeConfigService(
            JdbcTemplate jdbcTemplate,
            @Qualifier("gatewayProductJdbcTemplate") JdbcTemplate gatewayJdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.gatewayJdbcTemplate = gatewayJdbcTemplate;
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

    public PayRuntimeConfig getConfigForMerchant(String merchantNo) {
        if (!StringUtils.hasText(merchantNo)) {
            return getConfig();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM merchant_pay_config WHERE merchant_no = ?",
                merchantNo
        );
        if (rows.isEmpty()) {
            PayRuntimeConfig config = getConfig();
            config.setGatewayAppId(merchantNo);
            String appSecret = gatewayAppSecret(merchantNo);
            if (StringUtils.hasText(appSecret)) {
                config.setGatewayAppSecret(appSecret);
            }
            return config;
        }
        return fromMerchantRow(rows.get(0));
    }

    public PayRuntimeConfig getMaskedConfigForMerchant(String merchantNo) {
        PayRuntimeConfig config = getConfigForMerchant(merchantNo);
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

    public void updateMerchantConfig(String merchantNo, UpdatePayConfigRequest request) {
        if (!StringUtils.hasText(merchantNo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商家编号不存在");
        }
        PayRuntimeConfig current = getConfigForMerchant(merchantNo);
        String payChannel = value(request.getPayChannel(), current.getPayChannel());
        if (!PayChannel.MALLHOME.equals(payChannel) && !PayChannel.PAYMENT_GATEWAY.equals(payChannel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付通道不合法");
        }
        String gatewayAppId = StringUtils.hasText(request.getGatewayAppId()) ? request.getGatewayAppId().trim() : merchantNo;
        String gatewayAppSecret = secretValue(request.getGatewayAppSecret(), current.getGatewayAppSecret());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchant_pay_config WHERE merchant_no = ?",
                Integer.class,
                merchantNo
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(
                    """
                            UPDATE merchant_pay_config
                            SET pay_channel = ?, mallhome_host = ?, external_id = ?, notify_url = ?,
                                return_url = ?, default_pay_method_type = ?, gateway_host = ?,
                                gateway_app_id = ?, gateway_app_secret = ?, gateway_return_url = ?,
                                gateway_business_notify_url = ?, updated_at = NOW()
                            WHERE merchant_no = ?
                            """,
                    payChannel,
                    trimTrailingSlash(value(request.getHost(), current.getHost())),
                    value(request.getExternalId(), current.getExternalId()),
                    value(request.getNotifyUrl(), current.getNotifyUrl()),
                    value(request.getReturnUrl(), current.getReturnUrl()),
                    value(request.getDefaultPayMethodType(), current.getDefaultPayMethodType()),
                    trimTrailingSlash(value(request.getGatewayHost(), current.getGatewayHost())),
                    gatewayAppId,
                    gatewayAppSecret,
                    value(request.getGatewayReturnUrl(), current.getGatewayReturnUrl()),
                    value(request.getGatewayBusinessNotifyUrl(), current.getGatewayBusinessNotifyUrl()),
                    merchantNo
            );
            syncGatewayApp(gatewayAppId, gatewayAppSecret);
            return;
        }
        jdbcTemplate.update(
                """
                        INSERT INTO merchant_pay_config (
                            merchant_no, pay_channel, mallhome_host, external_id, notify_url, return_url,
                            default_pay_method_type, gateway_host, gateway_app_id, gateway_app_secret,
                            gateway_return_url, gateway_business_notify_url, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                merchantNo,
                payChannel,
                trimTrailingSlash(value(request.getHost(), current.getHost())),
                value(request.getExternalId(), current.getExternalId()),
                value(request.getNotifyUrl(), current.getNotifyUrl()),
                value(request.getReturnUrl(), current.getReturnUrl()),
                value(request.getDefaultPayMethodType(), current.getDefaultPayMethodType()),
                trimTrailingSlash(value(request.getGatewayHost(), current.getGatewayHost())),
                gatewayAppId,
                gatewayAppSecret,
                value(request.getGatewayReturnUrl(), current.getGatewayReturnUrl()),
                value(request.getGatewayBusinessNotifyUrl(), current.getGatewayBusinessNotifyUrl())
        );
        syncGatewayApp(gatewayAppId, gatewayAppSecret);
    }

    private PayRuntimeConfig fromMerchantRow(Map<String, Object> row) {
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

    public String gatewayAppSecret(String appId) {
        if (!StringUtils.hasText(appId)) {
            return "";
        }
        List<Map<String, Object>> rows = gatewayJdbcTemplate.queryForList(
                "SELECT app_secret FROM gateway_app WHERE app_id = ?",
                appId
        );
        if (rows.isEmpty()) {
            return "";
        }
        return string(rows.get(0), "app_secret");
    }

    private void syncGatewayApp(String appId, String appSecret) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret) || appSecret.contains("*")) {
            return;
        }
        List<Map<String, Object>> rows = gatewayJdbcTemplate.queryForList(
                "SELECT id FROM gateway_app WHERE app_id = ?",
                appId
        );
        if (rows.isEmpty()) {
            gatewayJdbcTemplate.update(
                    """
                            INSERT INTO gateway_app (
                                app_id, app_secret, app_name, enabled, notify_url, ip_whitelist, created_at, updated_at
                            ) VALUES (?, ?, ?, true, NULL, NULL, NOW(), NOW())
                            """,
                    appId,
                    appSecret,
                    appId
            );
            return;
        }
        gatewayJdbcTemplate.update(
                "UPDATE gateway_app SET app_secret = ?, enabled = true, updated_at = NOW() WHERE app_id = ?",
                appSecret,
                appId
        );
    }
}
