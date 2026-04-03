package com.rv.ecommerce.services;

import com.rv.ecommerce.account.AccountClient;
import com.rv.ecommerce.cashback.CashbackClient;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.exceptions.AccountOperationException;
import com.rv.ecommerce.exceptions.CashbackServiceException;
import com.rv.ecommerce.mappers.PaymentMapper;
import com.rv.ecommerce.repositories.PaymentTransferRepository;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private AccountClient accountClient;

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

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(PaymentTransfer.class))).thenReturn(response);

        assertThat(paymentService.transferToIndividual(request)).isEqualTo(response);

        ArgumentCaptor<UUID> transferIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(accountClient).applyTransfer(
                transferIdCaptor.capture(),
                eq("from1"),
                eq("to1"),
                eq(new BigDecimal("100.50")),
                eq(CurrencyCode.RUB)
        );
        assertThat(transferIdCaptor.getValue()).isNotNull();
        verify(cashbackClient, never()).notifyLegalEntityTransfer(any());
        verify(accountClient, never()).compensateTransfer(any());
    }

    @Test
    void transferToIndividual_whenAccountRejects_doesNotSaveAndDoesNotCompensate() {
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .build();

        doThrow(new AccountOperationException(409, "conflict"))
                .when(accountClient).applyTransfer(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> paymentService.transferToIndividual(request))
                .isInstanceOf(AccountOperationException.class);

        verify(paymentTransferRepository, never()).save(any());
        verify(accountClient, never()).compensateTransfer(any());
    }

    @Test
    void transferToIndividual_whenSaveFails_compensatesAccount() {
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> paymentService.transferToIndividual(request))
                .isInstanceOf(IllegalStateException.class);

        verify(accountClient).compensateTransfer(any(UUID.class));
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
                .status(PaymentStatus.COMPLETED)
                .legalEntityInn("1234567890")
                .legalEntityName("OOO Test")
                .cashbackNotified(true)
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

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(PaymentTransfer.class))).thenReturn(response);

        assertThat(paymentService.transferToLegalEntity(request)).isEqualTo(response);
        verify(cashbackClient).notifyLegalEntityTransfer(any());
        verify(paymentTransferRepository, times(2)).save(any(PaymentTransfer.class));
        verify(accountClient, never()).compensateTransfer(any());
    }

    @Test
    void transferToLegalEntity_cashbackFails_compensatesAccountAndMarksFailed() {
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

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new CashbackServiceException("down", new RuntimeException("http")))
                .when(cashbackClient).notifyLegalEntityTransfer(any());

        assertThatThrownBy(() -> paymentService.transferToLegalEntity(request))
                .isInstanceOf(CashbackServiceException.class);

        ArgumentCaptor<UUID> transferIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(accountClient).applyTransfer(
                transferIdCaptor.capture(),
                eq("from1"),
                eq("to1"),
                eq(new BigDecimal("50.00")),
                eq(CurrencyCode.EUR)
        );
        verify(accountClient).compensateTransfer(transferIdCaptor.getValue());
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

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(PaymentTransfer.class))).thenReturn(response);

        assertThat(paymentService.transferToLegalEntity(request)).isEqualTo(response);
        verify(cashbackClient, never()).notifyLegalEntityTransfer(any());
        verify(accountClient, never()).compensateTransfer(any());
    }

    @Test
    void transferToLegalEntity_whenSavePendingFailsAfterApply_compensates() {
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("a")
                .toAccountNumber("b")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .legalEntityInn("1111111111")
                .legalEntityName("OOO X")
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class)))
                .thenThrow(new IllegalStateException("db"));

        assertThatThrownBy(() -> paymentService.transferToLegalEntity(request))
                .isInstanceOf(IllegalStateException.class);

        verify(accountClient).compensateTransfer(any(UUID.class));
    }
}
