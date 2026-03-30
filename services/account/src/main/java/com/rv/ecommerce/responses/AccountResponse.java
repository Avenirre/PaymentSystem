package com.rv.ecommerce.responses;

import com.rv.ecommerce.entities.AccountEntity;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountResponse(
        String accountNumber,
        AccountEntity.CurrencyCode currency,
        AccountEntity.AccountStatus status,
        UUID ownerId,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt
) {
}
