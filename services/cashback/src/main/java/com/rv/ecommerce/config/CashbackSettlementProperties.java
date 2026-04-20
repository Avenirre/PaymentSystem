package com.rv.ecommerce.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "cashback.settlement")
@Validated
public record CashbackSettlementProperties(
        @NotNull Boolean enabled,
        @NotBlank String accountBaseUrl,
        @NotBlank String systemAccountNumber,
        @NotBlank String zoneId,
        @NotBlank String scheduleCron,
        @NotNull @DecimalMin("0.01") BigDecimal minimumPayoutAmount,
        /** If set, monthly cashback email notifications are sent to this address. */
        String payoutNotificationEmail
) {
}
