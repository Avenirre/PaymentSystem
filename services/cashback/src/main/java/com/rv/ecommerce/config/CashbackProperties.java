package com.rv.ecommerce.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "cashback.rates")
@Validated
public record CashbackProperties(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal individualDefaultPercent,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal legalEntityDefaultPercent
) {
}
