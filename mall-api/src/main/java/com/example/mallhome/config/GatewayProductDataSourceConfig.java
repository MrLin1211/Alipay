package com.example.mallhome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class GatewayProductDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "mallhome.gateway-datasource")
    public GatewayProductDataSourceProperties gatewayProductDataSourceProperties() {
        return new GatewayProductDataSourceProperties();
    }

    @Bean
    public JdbcTemplate gatewayProductJdbcTemplate(GatewayProductDataSourceProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        return new JdbcTemplate(dataSource);
    }
}
