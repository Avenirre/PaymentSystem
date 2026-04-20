package com.rv.ecommerce.services;

import tools.jackson.databind.json.JsonMapper;
import com.rv.ecommerce.account.AccountClient;
import com.rv.ecommerce.entities.CashbackOutbox;
import com.rv.ecommerce.entities.CashbackOutboxEventType;
import com.rv.ecommerce.entities.CashbackOutboxStatus;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.exceptions.AccountOperationException;
import com.rv.ecommerce.mappers.PaymentMapper;
import com.rv.ecommerce.notification.PaymentNotificationPublisher;
import com.rv.ecommerce.repositories.CashbackOutboxRepository;
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
import static org.mockito.ArgumentMatchers.argThat;
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
    private CashbackOutboxRepository cashbackOutboxRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private PaymentNotificationPublisher paymentNotificationPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(paymentService, "jsonMapper", JsonMapper.builder().findAndAddModules().build());
        ReflectionTestUtils.setField(paymentService, "cashbackEnabled", true);
        ReflectionTestUtils.setField(paymentService, "legalEntityTopic", "le-topic");
        ReflectionTestUtils.setField(paymentService, "individualTopic", "ind-topic");
    }

    @Test
    void transferToIndividual_cashbackSuccess_enqueuesOutboxAndCompletes() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("100.50"))
                .currency(CurrencyCode.RUB)
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.INDIVIDUAL)
                .status(PaymentStatus.COMPLETED)
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("100.50"))
                .currency(CurrencyCode.RUB)
                .cashbackNotified(true)
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
        verify(cashbackOutboxRepository).save(argThat((CashbackOutbox o) ->
                o.getEventType() == CashbackOutboxEventType.INDIVIDUAL_CASHBACK
                        && o.getStatus() == CashbackOutboxStatus.PENDING
                        && o.getTopic().equals("ind-topic")));
        verify(paymentTransferRepository, times(2)).save(any(PaymentTransfer.class));
        verify(accountClient, never()).compensateTransfer(any());
        verify(paymentNotificationPublisher).publishPaymentCompleted(any(PaymentTransfer.class), eq(request));
    }

    @Test
    void transferToIndividual_whenAccountRejects_doesNotSaveAndDoesNotCompensate() {
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        doThrow(new AccountOperationException(409, "conflict"))
                .when(accountClient).applyTransfer(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> paymentService.transferToIndividual(request))
                .isInstanceOf(AccountOperationException.class);

        verify(paymentTransferRepository, never()).save(any());
        verify(accountClient, never()).compensateTransfer(any());
        verify(paymentNotificationPublisher, never()).publishPaymentCompleted(
                any(PaymentTransfer.class), any(IndividualTransferRequest.class));
    }

    @Test
    void transferToIndividual_whenSaveFails_compensatesAccount() {
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> paymentService.transferToIndividual(request))
                .isInstanceOf(IllegalStateException.class);

        verify(accountClient).compensateTransfer(any(UUID.class));
        verify(paymentNotificationPublisher, never()).publishPaymentCompleted(
                any(PaymentTransfer.class), any(IndividualTransferRequest.class));
    }

    @Test
    void transferToIndividual_whenOutboxSaveFails_compensatesAccount() {
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("50.00"))
                .currency(CurrencyCode.EUR)
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("outbox"))
                .when(cashbackOutboxRepository).save(any(CashbackOutbox.class));

        assertThatThrownBy(() -> paymentService.transferToIndividual(request))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<UUID> transferIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(accountClient).applyTransfer(
                transferIdCaptor.capture(),
                eq("from1"),
                eq("to1"),
                eq(new BigDecimal("50.00")),
                eq(CurrencyCode.EUR)
        );
        verify(accountClient).compensateTransfer(transferIdCaptor.getValue());
        verify(paymentTransferRepository, times(1)).save(any(PaymentTransfer.class));
        verify(paymentNotificationPublisher, never()).publishPaymentCompleted(
                any(PaymentTransfer.class), any(IndividualTransferRequest.class));
    }

    @Test
    void transferToIndividual_cashbackDisabled_skipsOutbox() {
        ReflectionTestUtils.setField(paymentService, "cashbackEnabled", false);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("a")
                .toAccountNumber("b")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.INDIVIDUAL)
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

        assertThat(paymentService.transferToIndividual(request)).isEqualTo(response);
        verify(cashbackOutboxRepository, never()).save(any());
        verify(accountClient, never()).compensateTransfer(any());
        verify(paymentNotificationPublisher).publishPaymentCompleted(any(PaymentTransfer.class), eq(request));
    }

    @Test
    void transferToLegalEntity_cashbackSuccess_enqueuesOutboxAndCompletes() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("200.00"))
                .currency(CurrencyCode.RUB)
                .legalEntityInn("1234567890")
                .legalEntityName("OOO Test")
                .senderEmail(null)
                .recipientEmail(null)
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
        verify(cashbackOutboxRepository).save(argThat((CashbackOutbox o) ->
                o.getEventType() == CashbackOutboxEventType.LEGAL_ENTITY_CASHBACK
                        && o.getStatus() == CashbackOutboxStatus.PENDING
                        && o.getTopic().equals("le-topic")));
        verify(paymentTransferRepository, times(2)).save(any(PaymentTransfer.class));
        verify(accountClient, never()).compensateTransfer(any());
        verify(paymentNotificationPublisher).publishPaymentCompleted(any(PaymentTransfer.class), eq(request));
    }

    @Test
    void transferToLegalEntity_whenOutboxSaveFails_compensatesAccount() {
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("from1")
                .toAccountNumber("to1")
                .amount(new BigDecimal("50.00"))
                .currency(CurrencyCode.EUR)
                .legalEntityInn("0987654321")
                .legalEntityName("OOO Fail")
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("outbox"))
                .when(cashbackOutboxRepository).save(any(CashbackOutbox.class));

        assertThatThrownBy(() -> paymentService.transferToLegalEntity(request))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<UUID> transferIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(accountClient).applyTransfer(
                transferIdCaptor.capture(),
                eq("from1"),
                eq("to1"),
                eq(new BigDecimal("50.00")),
                eq(CurrencyCode.EUR)
        );
        verify(accountClient).compensateTransfer(transferIdCaptor.getValue());
        verify(paymentTransferRepository, times(1)).save(any(PaymentTransfer.class));
        verify(paymentNotificationPublisher, never()).publishPaymentCompleted(
                any(PaymentTransfer.class), any(LegalEntityTransferRequest.class));
    }

    @Test
    void transferToLegalEntity_cashbackDisabled_skipsOutbox() {
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
                .senderEmail(null)
                .recipientEmail(null)
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
        verify(cashbackOutboxRepository, never()).save(any());
        verify(accountClient, never()).compensateTransfer(any());
        verify(paymentNotificationPublisher).publishPaymentCompleted(any(PaymentTransfer.class), eq(request));
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
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        when(paymentTransferRepository.save(any(PaymentTransfer.class)))
                .thenThrow(new IllegalStateException("db"));

        assertThatThrownBy(() -> paymentService.transferToLegalEntity(request))
                .isInstanceOf(IllegalStateException.class);

        verify(accountClient).compensateTransfer(any(UUID.class));
        verify(paymentNotificationPublisher, never()).publishPaymentCompleted(
                any(PaymentTransfer.class), any(LegalEntityTransferRequest.class));
    }
}
