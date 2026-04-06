package com.rv.ecommerce.kafka;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Must match JSON from payment-service ({@code CashbackKafkaProducer#publishLegalEntityTransfer}).
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
