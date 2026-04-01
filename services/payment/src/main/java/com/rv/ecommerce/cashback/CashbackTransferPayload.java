package com.rv.ecommerce.cashback;

import java.math.BigDecimal;
import java.util.UUID;

public record CashbackTransferPayload(
        UUID transferId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currencyCode,
        String legalEntityInn,
        String legalEntityName
) {
}
