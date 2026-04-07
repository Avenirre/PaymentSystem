package com.rv.ecommerce.account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JSON body for {@code POST /api/v1/account/transfers/apply} (mirrors account-service {@code ApplyTransferRequest}).
 */
public record AccountApplyTransferRequest(
        UUID transferId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currency
) {
}
