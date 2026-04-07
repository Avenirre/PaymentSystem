package com.rv.ecommerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 wires Jackson 3 ({@code JsonMapper}) for Web; {@code PaymentService} / outbox still use
 * {@code com.fasterxml.jackson.databind.ObjectMapper} for JSON payloads — register an application bean.
 */
@Configuration
public class PaymentObjectMapperConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
