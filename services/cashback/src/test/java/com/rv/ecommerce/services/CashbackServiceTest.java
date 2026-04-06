package com.rv.ecommerce.services;

import com.rv.ecommerce.config.CashbackProperties;
import com.rv.ecommerce.entities.CashbackDocument;
import com.rv.ecommerce.entities.CashbackType;
import com.rv.ecommerce.repositories.CashbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashbackServiceTest {

    @Mock
    private CashbackRepository cashbackRepository;

    @Mock
    private CashbackProperties defaultPercents;

    @InjectMocks
    private CashbackService cashbackService;

    @Test
    void getEffectivePercent_whenNoDocument_usesConfiguration() {
        when(cashbackRepository.findById("INDIVIDUAL")).thenReturn(Optional.empty());
        when(defaultPercents.individualDefaultPercent()).thenReturn(new BigDecimal("1.25"));

        assertThat(cashbackService.getEffectivePercent(CashbackType.INDIVIDUAL))
                .isEqualByComparingTo("1.25");
    }

    @Test
    void getEffectivePercent_whenDocumentExists_usesDatabase() {
        when(cashbackRepository.findById("LEGAL_ENTITY")).thenReturn(Optional.of(
                CashbackDocument.builder()
                        .id("LEGAL_ENTITY")
                        .percent(new BigDecimal("3.5"))
                        .build()
        ));

        assertThat(cashbackService.getEffectivePercent(CashbackType.LEGAL_ENTITY))
                .isEqualByComparingTo("3.5");
    }

    @Test
    void getEffectivePercent_whenDocumentHasNullPercent_usesConfiguration() {
        when(cashbackRepository.findById("INDIVIDUAL")).thenReturn(Optional.of(
                CashbackDocument.builder()
                        .id("INDIVIDUAL")
                        .percent(null)
                        .build()
        ));
        when(defaultPercents.individualDefaultPercent()).thenReturn(new BigDecimal("2.0"));

        assertThat(cashbackService.getEffectivePercent(CashbackType.INDIVIDUAL))
                .isEqualByComparingTo("2.0");
    }
}
