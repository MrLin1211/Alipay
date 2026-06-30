package com.example.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class MallhomeDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "mallhome.datasource")
    public MallhomeDataSourceProperties mallhomeDataSourceProperties() {
        return new MallhomeDataSourceProperties();
    }

    @Bean
    public JdbcTemplate mallhomeJdbcTemplate(MallhomeDataSourceProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        return new JdbcTemplate(dataSource);
    }
}
