package com.rv.ecommerce.kafka;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload for cashback (legal-entity transfer), published to Kafka.
 */
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
