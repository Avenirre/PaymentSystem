package com.rv.ecommerce.kafka;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload for individual (P2P) transfer cashback, published to Kafka.
 */
public record IndividualCashbackPayload(
        UUID transferId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currencyCode
) {
}
