package com.rv.ecommerce;

import com.rv.ecommerce.config.CashbackProperties;
import com.rv.ecommerce.config.CashbackSettlementProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({CashbackProperties.class, CashbackSettlementProperties.class})
public class CashbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashbackApplication.class, args);
    }
}
