package com.rv.ecommerce.requests;

import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record LegalEntityTransferRequest(
        @NotBlank String fromAccountNumber,
        @NotBlank String toAccountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotNull CurrencyCode currency,
        @NotBlank @Size(min = 10, max = 12) String legalEntityInn,
        @NotBlank String legalEntityName
) {
}
