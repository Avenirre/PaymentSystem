package com.rv.ecommerce.services;

import com.rv.ecommerce.account.AccountClient;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.exceptions.CashbackServiceException;
import com.rv.ecommerce.kafka.CashbackKafkaProducer;
import com.rv.ecommerce.kafka.CashbackTransferPayload;
import com.rv.ecommerce.mappers.PaymentMapper;
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

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransferRepository paymentTransferRepository;
    private final PaymentMapper paymentMapper;
    private final CashbackKafkaProducer cashbackKafkaProducer;
    private final AccountClient accountClient;

    @Value("${cashback.enabled:true}")
    private boolean cashbackEnabled;

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
                    .status(PaymentStatus.COMPLETED)
                    .cashbackNotified(false)
                    .build();
            PaymentTransfer saved = paymentTransferRepository.save(entity);
            log.info("Individual transfer completed transferId={}", saved.getId());
            return paymentMapper.toResponse(saved);
        } catch (RuntimeException e) {
            if (accountApplied) {
                compensateSafely(transferId);
            }
            throw e;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, noRollbackFor = CashbackServiceException.class)
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
                try {
                    cashbackKafkaProducer.publishLegalEntityTransfer(new CashbackTransferPayload(
                            saved.getId(),
                            saved.getFromAccountNumber(),
                            saved.getToAccountNumber(),
                            saved.getAmount(),
                            saved.getCurrency().name(),
                            saved.getLegalEntityInn(),
                            saved.getLegalEntityName()
                    ));
                    saved.setStatus(PaymentStatus.COMPLETED);
                    saved.setCashbackNotified(true);
                    paymentTransferRepository.save(saved);
                    log.info("Legal-entity transfer completed with cashback Kafka event transferId={}", saved.getId());
                } catch (CashbackServiceException exception) {
                    accountClient.compensateTransfer(transferId);
                    saved.setStatus(PaymentStatus.FAILED);
                    paymentTransferRepository.save(saved);
                    log.warn("Legal-entity transfer failed cashback Kafka, account compensated transferId={}", saved.getId());
                    throw exception;
                }
            } else {
                saved.setStatus(PaymentStatus.COMPLETED);
                paymentTransferRepository.save(saved);
                log.info("Legal-entity transfer completed without cashback (disabled) transferId={}", saved.getId());
            }

            return paymentMapper.toResponse(saved);
        } catch (CashbackServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            if (accountApplied) {
                compensateSafely(transferId);
            }
            throw e;
        }
    }

    private void compensateSafely(UUID transferId) {
        try {
            accountClient.compensateTransfer(transferId);
        } catch (Exception ex) {
            log.error("Failed to compensate account transfer transferId={}", transferId, ex);
        }
    }
}
