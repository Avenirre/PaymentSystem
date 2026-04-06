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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashbackServiceTest {

    @Mock
    private CashbackRepository cashbackRepository;

    @Mock
    private CashbackProperties defaultPercents;

    @Mock
    private LegalEntityCashbackAccrualRepository legalEntityCashbackAccrualRepository;

    @Mock
    private IndividualCashbackAccrualRepository individualCashbackAccrualRepository;

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

    @Test
    void processLegalEntityTransfer_savesAccrualWithPercentFromConfiguration() {
        UUID transferId = UUID.randomUUID();
        var payload = new CashbackTransferPayload(
                transferId,
                "from",
                "to",
                new BigDecimal("1000.00"),
                "RUB",
                "1234567890",
                "OOO Test"
        );
        when(legalEntityCashbackAccrualRepository.existsById(transferId)).thenReturn(false);
        when(cashbackRepository.findById("LEGAL_ENTITY")).thenReturn(Optional.empty());
        when(defaultPercents.legalEntityDefaultPercent()).thenReturn(new BigDecimal("0.5"));

        ArgumentCaptor<LegalEntityCashbackAccrualDocument> captor =
                ArgumentCaptor.forClass(LegalEntityCashbackAccrualDocument.class);
        cashbackService.processLegalEntityTransfer(payload);

        verify(legalEntityCashbackAccrualRepository).save(captor.capture());
        LegalEntityCashbackAccrualDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(transferId);
        assertThat(saved.getCashbackAmount()).isEqualByComparingTo("5.00");
        assertThat(saved.getCashbackPercent()).isEqualByComparingTo("0.5");
    }

    @Test
    void processLegalEntityTransfer_whenAlreadyProcessed_skipsSave() {
        UUID transferId = UUID.randomUUID();
        var payload = new CashbackTransferPayload(
                transferId, "a", "b", BigDecimal.ONE, "RUB", "1", "X");
        when(legalEntityCashbackAccrualRepository.existsById(transferId)).thenReturn(true);

        cashbackService.processLegalEntityTransfer(payload);

        verify(legalEntityCashbackAccrualRepository, never()).save(any());
    }

    @Test
    void processIndividualTransfer_savesAccrualWithPercentFromConfiguration() {
        UUID transferId = UUID.randomUUID();
        var payload = new IndividualCashbackPayload(
                transferId,
                "from",
                "to",
                new BigDecimal("1000.00"),
                "RUB"
        );
        when(individualCashbackAccrualRepository.existsById(transferId)).thenReturn(false);
        when(cashbackRepository.findById("INDIVIDUAL")).thenReturn(Optional.empty());
        when(defaultPercents.individualDefaultPercent()).thenReturn(new BigDecimal("1.0"));

        ArgumentCaptor<IndividualCashbackAccrualDocument> captor =
                ArgumentCaptor.forClass(IndividualCashbackAccrualDocument.class);
        cashbackService.processIndividualTransfer(payload);

        verify(individualCashbackAccrualRepository).save(captor.capture());
        IndividualCashbackAccrualDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(transferId);
        assertThat(saved.getCashbackAmount()).isEqualByComparingTo("10.00");
        assertThat(saved.getCashbackPercent()).isEqualByComparingTo("1.0");
    }

    @Test
    void processIndividualTransfer_whenAlreadyProcessed_skipsSave() {
        UUID transferId = UUID.randomUUID();
        var payload = new IndividualCashbackPayload(
                transferId, "a", "b", BigDecimal.ONE, "RUB");
        when(individualCashbackAccrualRepository.existsById(transferId)).thenReturn(true);

        cashbackService.processIndividualTransfer(payload);

        verify(individualCashbackAccrualRepository, never()).save(any());
    }
}
