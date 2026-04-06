package com.rv.ecommerce;

import com.rv.ecommerce.config.CashbackProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CashbackProperties.class)
public class CashbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashbackApplication.class, args);
    }
}
