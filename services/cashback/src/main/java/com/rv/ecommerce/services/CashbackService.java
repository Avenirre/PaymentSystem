package com.rv.ecommerce.services;

import com.rv.ecommerce.config.CashbackProperties;
import com.rv.ecommerce.entities.CashbackDocument;
import com.rv.ecommerce.entities.CashbackType;
import com.rv.ecommerce.entities.IndividualCashbackAccrualDocument;
import com.rv.ecommerce.entities.LegalEntityCashbackAccrualDocument;
import com.rv.ecommerce.kafka.CashbackTransferPayload;
import com.rv.ecommerce.kafka.IndividualCashbackPayload;
import com.rv.ecommerce.repositories.CashbackRepository;
import com.rv.ecommerce.repositories.IndividualCashbackAccrualRepository;
import com.rv.ecommerce.repositories.LegalEntityCashbackAccrualRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CashbackService {

    private final CashbackRepository cashbackRepository;
    private final LegalEntityCashbackAccrualRepository legalEntityCashbackAccrualRepository;
    private final IndividualCashbackAccrualRepository individualCashbackAccrualRepository;
    private final CashbackProperties defaultPercents;

    /**
     * Effective percent: value from MongoDB if a document exists with a non-null percent;
     * otherwise from application configuration ({@link CashbackProperties}).
     */
    public BigDecimal getEffectivePercent(CashbackType cashbackType) {
        return cashbackRepository.findById(cashbackType.name())
                .map(CashbackDocument::getPercent)
                .orElseGet(() -> defaultPercentFromConfiguration(cashbackType));
    }

    public CashbackDocument updatePercent(CashbackType cashbackType, BigDecimal percent) {
        CashbackDocument doc = CashbackDocument.builder()
                .id(cashbackType.name())
                .percent(percent)
                .build();
        return cashbackRepository.save(doc);
    }

    /**
     * Persists accrual for a legal-entity transfer event from Kafka (idempotent by {@code transferId}).
     */
    public void processLegalEntityTransfer(CashbackTransferPayload payload) {
        if (legalEntityCashbackAccrualRepository.existsById(payload.transferId())) {
            return;
        }
        BigDecimal percent = getEffectivePercent(CashbackType.LEGAL_ENTITY);
        BigDecimal cashbackAmount = payload.amount()
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        LegalEntityCashbackAccrualDocument doc = LegalEntityCashbackAccrualDocument.builder()
                .id(payload.transferId())
                .fromAccountNumber(payload.fromAccountNumber())
                .toAccountNumber(payload.toAccountNumber())
                .transferAmount(payload.amount())
                .currencyCode(payload.currencyCode())
                .legalEntityInn(payload.legalEntityInn())
                .legalEntityName(payload.legalEntityName())
                .cashbackPercent(percent)
                .cashbackAmount(cashbackAmount)
                .createdAt(Instant.now())
                .build();
        legalEntityCashbackAccrualRepository.save(doc);
    }

    /**
     * Persists accrual for an individual (P2P) transfer event from Kafka (idempotent by {@code transferId}).
     */
    public void processIndividualTransfer(IndividualCashbackPayload payload) {
        if (individualCashbackAccrualRepository.existsById(payload.transferId())) {
            return;
        }
        BigDecimal percent = getEffectivePercent(CashbackType.INDIVIDUAL);
        BigDecimal cashbackAmount = payload.amount()
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        IndividualCashbackAccrualDocument doc = IndividualCashbackAccrualDocument.builder()
                .id(payload.transferId())
                .fromAccountNumber(payload.fromAccountNumber())
                .toAccountNumber(payload.toAccountNumber())
                .transferAmount(payload.amount())
                .currencyCode(payload.currencyCode())
                .cashbackPercent(percent)
                .cashbackAmount(cashbackAmount)
                .createdAt(Instant.now())
                .build();
        individualCashbackAccrualRepository.save(doc);
    }

    private BigDecimal defaultPercentFromConfiguration(CashbackType cashbackType) {
        return switch (cashbackType) {
            case INDIVIDUAL -> defaultPercents.individualDefaultPercent();
            case LEGAL_ENTITY -> defaultPercents.legalEntityDefaultPercent();
        };
    }
}
