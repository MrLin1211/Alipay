package com.example.mallhome;

import com.example.mallhome.config.MallhomePayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MallhomePayProperties.class)
public class MallhomePayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallhomePayApplication.class, args);
    }
}
