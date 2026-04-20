package com.rv.ecommerce.requests;

import com.rv.ecommerce.entities.AccountEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AccountRequest(
        @NotNull
        UUID ownerId,
        @NotNull
        AccountEntity.CurrencyCode currency,
        /** Optional: for email notifications. */
        @Email
        String ownerEmail
) {
}
