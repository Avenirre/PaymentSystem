package com.rv.ecommerce.requests;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateCashbackRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percent
) {
}
