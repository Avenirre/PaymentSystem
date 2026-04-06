package com.rv.ecommerce.services;

import com.rv.ecommerce.config.CashbackProperties;
import com.rv.ecommerce.entities.CashbackDocument;
import com.rv.ecommerce.entities.CashbackType;
import com.rv.ecommerce.repositories.CashbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashbackService {

    private final CashbackRepository cashbackRepository;
    private final CashbackProperties defaultPercents;

    /**
     * Effective percent: value from MongoDB if a document exists with a non-null percent;
     * otherwise from application configuration ({@link CashbackProperties}).
     */
    public BigDecimal getEffectivePercent(CashbackType cashbackType) {
        return cashbackRepository.findById(cashbackType.name())
                .map(CashbackDocument::getPercent)
                .filter(p -> p != null)
                .orElseGet(() -> defaultPercentFromConfiguration(cashbackType));
    }

    public CashbackDocument updatePercent(CashbackType cashbackType, BigDecimal percent) {
        CashbackDocument doc = CashbackDocument.builder()
                .id(cashbackType.name())
                .percent(percent)
                .build();
        return cashbackRepository.save(doc);
    }

    private BigDecimal defaultPercentFromConfiguration(CashbackType cashbackType) {
        return switch (cashbackType) {
            case INDIVIDUAL -> defaultPercents.individualDefaultPercent();
            case LEGAL_ENTITY -> defaultPercents.legalEntityDefaultPercent();
        };
    }
}
