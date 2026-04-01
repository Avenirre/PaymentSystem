package com.rv.ecommerce.responses;

import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentTransferResponse(
        UUID transferId,
        TransferType transferType,
        PaymentStatus status,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        CurrencyCode currency,
        boolean cashbackNotified,
        Instant createdAt
) {
}
