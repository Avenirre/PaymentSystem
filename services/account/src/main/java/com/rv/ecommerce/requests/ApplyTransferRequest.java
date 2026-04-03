package com.rv.ecommerce.requests;

import com.rv.ecommerce.entities.AccountEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ApplyTransferRequest(
        @NotNull UUID transferId,
        @NotBlank String fromAccountNumber,
        @NotBlank String toAccountNumber,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull AccountEntity.CurrencyCode currency
) {
}
