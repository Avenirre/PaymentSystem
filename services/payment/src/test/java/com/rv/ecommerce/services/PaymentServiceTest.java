package com.rv.ecommerce.services;

import com.rv.ecommerce.cashback.CashbackClient;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.exceptions.CashbackServiceException;
import com.rv.ecommerce.mappers.PaymentMapper;
import com.rv.ecommerce.repositories.PaymentTransferRepository;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransferRepository paymentTransferRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private CashbackClient cashbackClient;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void enableCashbackByDefault() {
        ReflectionTestUtils.setField(paymentService, "cashbackEnabled", true);
    }

    @Test
    void transferToIndividual_success() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("100.50"))
                .currency(CurrencyCode.RUB)
                .build();

        PaymentTransfer saved = PaymentTransfer.builder()
                .id(id)
                .transferType(TransferType.INDIVIDUAL)
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("100.50"))
                .currency(CurrencyCode.RUB)
                .status(PaymentStatus.COMPLETED)
                .cashbackNotified(false)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.INDIVIDUAL)
                .status(PaymentStatus.COMPLETED)
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("100.50"))
                .currency(CurrencyCode.RUB)
                .cashbackNotified(false)
                .createdAt(now)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenReturn(saved);
        when(paymentMapper.toResponse(saved)).thenReturn(response);

        assertThat(paymentService.transferToIndividual(request)).isEqualTo(response);
        verify(cashbackClient, never()).notifyLegalEntityTransfer(any());
    }

    @Test
    void transferToLegalEntity_cashbackSuccess_completesAndNotifies() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("200.00"))
                .currency(CurrencyCode.RUB)
                .legalEntityInn("1234567890")
                .legalEntityName("OOO Test")
                .build();

        PaymentTransfer saved = PaymentTransfer.builder()
                .id(id)
                .transferType(TransferType.LEGAL_ENTITY)
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("200.00"))
                .currency(CurrencyCode.RUB)
                .status(PaymentStatus.PENDING)
                .legalEntityInn("1234567890")
                .legalEntityName("OOO Test")
                .cashbackNotified(false)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.LEGAL_ENTITY)
                .status(PaymentStatus.COMPLETED)
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("200.00"))
                .currency(CurrencyCode.RUB)
                .cashbackNotified(true)
                .createdAt(now)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenReturn(saved);
        when(paymentMapper.toResponse(any(PaymentTransfer.class))).thenReturn(response);

        assertThat(paymentService.transferToLegalEntity(request)).isEqualTo(response);
        verify(cashbackClient).notifyLegalEntityTransfer(any());
        verify(paymentTransferRepository, times(2)).save(any(PaymentTransfer.class));
    }

    @Test
    void transferToLegalEntity_cashbackFails_marksFailedAndThrows() {
        ReflectionTestUtils.setField(paymentService, "cashbackEnabled", true);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("50.00"))
                .currency(CurrencyCode.EUR)
                .legalEntityInn("0987654321")
                .legalEntityName("OOO Fail")
                .build();

        PaymentTransfer saved = PaymentTransfer.builder()
                .id(id)
                .transferType(TransferType.LEGAL_ENTITY)
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("50.00"))
                .currency(CurrencyCode.EUR)
                .status(PaymentStatus.PENDING)
                .legalEntityInn("0987654321")
                .legalEntityName("OOO Fail")
                .cashbackNotified(false)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenReturn(saved);
        doThrow(new CashbackServiceException("down", new RuntimeException("http")))
                .when(cashbackClient).notifyLegalEntityTransfer(any());

        assertThatThrownBy(() -> paymentService.transferToLegalEntity(request))
                .isInstanceOf(CashbackServiceException.class);

        verify(paymentTransferRepository, times(2)).save(any(PaymentTransfer.class));
    }

    @Test
    void transferToLegalEntity_cashbackDisabled_skipsClient() {
        ReflectionTestUtils.setField(paymentService, "cashbackEnabled", false);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("a")
                .toAccountNumber("b")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .legalEntityInn("1111111111")
                .legalEntityName("OOO X")
                .build();

        PaymentTransfer saved = PaymentTransfer.builder()
                .id(id)
                .transferType(TransferType.LEGAL_ENTITY)
                .fromAccountNumber("a")
                .toAccountNumber("b")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .status(PaymentStatus.COMPLETED)
                .legalEntityInn("1111111111")
                .legalEntityName("OOO X")
                .cashbackNotified(false)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.LEGAL_ENTITY)
                .status(PaymentStatus.COMPLETED)
                .fromAccountNumber("a")
                .toAccountNumber("b")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .cashbackNotified(false)
                .createdAt(now)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenReturn(saved);
        when(paymentMapper.toResponse(saved)).thenReturn(response);

        assertThat(paymentService.transferToLegalEntity(request)).isEqualTo(response);
        verify(cashbackClient, never()).notifyLegalEntityTransfer(any());
    }
}
