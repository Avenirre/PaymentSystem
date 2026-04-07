package com.rv.ecommerce.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rv.ecommerce.account.AccountClient;
import com.rv.ecommerce.entities.CashbackOutbox;
import com.rv.ecommerce.entities.CashbackOutboxEventType;
import com.rv.ecommerce.entities.CashbackOutboxStatus;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.kafka.CashbackTransferPayload;
import com.rv.ecommerce.kafka.IndividualCashbackPayload;
import com.rv.ecommerce.mappers.PaymentMapper;
import com.rv.ecommerce.repositories.CashbackOutboxRepository;
import com.rv.ecommerce.repositories.PaymentTransferRepository;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransferRepository paymentTransferRepository;
    private final PaymentMapper paymentMapper;
    private final CashbackOutboxRepository cashbackOutboxRepository;
    private final ObjectMapper objectMapper;
    private final AccountClient accountClient;

    @Value("${cashback.enabled:true}")
    private boolean cashbackEnabled;

    @Value("${cashback.kafka.legal-entity-topic}")
    private String legalEntityTopic;

    @Value("${cashback.kafka.individual-topic}")
    private String individualTopic;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentTransferResponse transferToIndividual(IndividualTransferRequest request) {
        UUID transferId = UUID.randomUUID();
        boolean accountApplied = false;
        try {
            accountClient.applyTransfer(
                    transferId,
                    request.fromAccountNumber(),
                    request.toAccountNumber(),
                    request.amount(),
                    request.currency()
            );
            accountApplied = true;

            PaymentTransfer entity = PaymentTransfer.builder()
                    .id(transferId)
                    .transferType(TransferType.INDIVIDUAL)
                    .fromAccountNumber(request.fromAccountNumber())
                    .toAccountNumber(request.toAccountNumber())
                    .amount(request.amount())
                    .currency(request.currency())
                    .status(PaymentStatus.PENDING)
                    .cashbackNotified(false)
                    .build();
            PaymentTransfer saved = paymentTransferRepository.save(entity);

            if (cashbackEnabled) {
                enqueueIndividualCashback(saved);
                saved.setStatus(PaymentStatus.COMPLETED);
                saved.setCashbackNotified(true);
                paymentTransferRepository.save(saved);
                log.info("Individual transfer completed, cashback outbox enqueued transferId={}", saved.getId());
            } else {
                saved.setStatus(PaymentStatus.COMPLETED);
                paymentTransferRepository.save(saved);
                log.info("Individual transfer completed without cashback (disabled) transferId={}", saved.getId());
            }

            return paymentMapper.toResponse(saved);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cashback outbox payload", e);
        } catch (RuntimeException e) {
            if (accountApplied) {
                compensateSafely(transferId);
            }
            throw e;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentTransferResponse transferToLegalEntity(LegalEntityTransferRequest request) {
        UUID transferId = UUID.randomUUID();
        boolean accountApplied = false;
        try {
            accountClient.applyTransfer(
                    transferId,
                    request.fromAccountNumber(),
                    request.toAccountNumber(),
                    request.amount(),
                    request.currency()
            );
            accountApplied = true;

            PaymentTransfer entity = PaymentTransfer.builder()
                    .id(transferId)
                    .transferType(TransferType.LEGAL_ENTITY)
                    .fromAccountNumber(request.fromAccountNumber())
                    .toAccountNumber(request.toAccountNumber())
                    .amount(request.amount())
                    .currency(request.currency())
                    .status(PaymentStatus.PENDING)
                    .legalEntityInn(request.legalEntityInn())
                    .legalEntityName(request.legalEntityName())
                    .cashbackNotified(false)
                    .build();
            PaymentTransfer saved = paymentTransferRepository.save(entity);

            if (cashbackEnabled) {
                enqueueLegalEntityCashback(saved);
                saved.setStatus(PaymentStatus.COMPLETED);
                saved.setCashbackNotified(true);
                paymentTransferRepository.save(saved);
                log.info("Legal-entity transfer completed, cashback outbox enqueued transferId={}", saved.getId());
            } else {
                saved.setStatus(PaymentStatus.COMPLETED);
                paymentTransferRepository.save(saved);
                log.info("Legal-entity transfer completed without cashback (disabled) transferId={}", saved.getId());
            }

            return paymentMapper.toResponse(saved);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cashback outbox payload", e);
        } catch (RuntimeException e) {
            if (accountApplied) {
                compensateSafely(transferId);
            }
            throw e;
        }
    }

    private void enqueueIndividualCashback(PaymentTransfer saved) throws JsonProcessingException {
        IndividualCashbackPayload payload = new IndividualCashbackPayload(
                saved.getId(),
                saved.getFromAccountNumber(),
                saved.getToAccountNumber(),
                saved.getAmount(),
                saved.getCurrency().name()
        );
        cashbackOutboxRepository.save(CashbackOutbox.builder()
                .id(UUID.randomUUID())
                .aggregateId(saved.getId())
                .eventType(CashbackOutboxEventType.INDIVIDUAL_CASHBACK)
                .topic(individualTopic)
                .partitionKey(saved.getId().toString())
                .payloadJson(objectMapper.writeValueAsString(payload))
                .status(CashbackOutboxStatus.PENDING)
                .createdAt(Instant.now())
                .attemptCount(0)
                .build());
    }

    private void enqueueLegalEntityCashback(PaymentTransfer saved) throws JsonProcessingException {
        CashbackTransferPayload payload = new CashbackTransferPayload(
                saved.getId(),
                saved.getFromAccountNumber(),
                saved.getToAccountNumber(),
                saved.getAmount(),
                saved.getCurrency().name(),
                saved.getLegalEntityInn(),
                saved.getLegalEntityName()
        );
        cashbackOutboxRepository.save(CashbackOutbox.builder()
                .id(UUID.randomUUID())
                .aggregateId(saved.getId())
                .eventType(CashbackOutboxEventType.LEGAL_ENTITY_CASHBACK)
                .topic(legalEntityTopic)
                .partitionKey(saved.getId().toString())
                .payloadJson(objectMapper.writeValueAsString(payload))
                .status(CashbackOutboxStatus.PENDING)
                .createdAt(Instant.now())
                .attemptCount(0)
                .build());
    }

    private void compensateSafely(UUID transferId) {
        try {
            accountClient.compensateTransfer(transferId);
        } catch (Exception ex) {
            log.error("Failed to compensate account transfer transferId={}", transferId, ex);
        }
    }
}
