package com.rv.ecommerce.requests;

import com.rv.ecommerce.entities.AccountEntity;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AccountRequest(
        UUID ownerId,
        AccountEntity.CurrencyCode currency
) {
}
