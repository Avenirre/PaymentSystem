package com.rv.ecommerce.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "legal_entity_cashback_accruals")
public class LegalEntityCashbackAccrualDocument {

    @Id
    private UUID id;

    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal transferAmount;
    private String currencyCode;
    private String legalEntityInn;
    private String legalEntityName;
    private BigDecimal cashbackPercent;
    private BigDecimal cashbackAmount;
    private Instant createdAt;
}
