package com.rv.ecommerce.account;

import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyAccountTransferPayload(
        UUID transferId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        CurrencyCode currency
) {
}
