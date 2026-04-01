package com.rv.ecommerce.services;

import com.rv.ecommerce.cashback.CashbackClient;
import com.rv.ecommerce.cashback.CashbackTransferPayload;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.mappers.PaymentMapper;
import com.rv.ecommerce.exceptions.CashbackServiceException;
import com.rv.ecommerce.repositories.PaymentTransferRepository;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransferRepository paymentTransferRepository;
    private final PaymentMapper paymentMapper;
    private final CashbackClient cashbackClient;

    @Value("${cashback.enabled:true}")
    private boolean cashbackEnabled;

    @Transactional
    public PaymentTransferResponse transferToIndividual(IndividualTransferRequest request) {
        PaymentTransfer entity = PaymentTransfer.builder()
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
    }

    @Transactional(noRollbackFor = CashbackServiceException.class)
    public PaymentTransferResponse transferToLegalEntity(LegalEntityTransferRequest request) {
        PaymentTransfer entity = PaymentTransfer.builder()
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
                cashbackClient.notifyLegalEntityTransfer(new CashbackTransferPayload(
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
                log.info("Legal-entity transfer completed with cashback notification transferId={}", saved.getId());
            } catch (CashbackServiceException exception) {
                saved.setStatus(PaymentStatus.FAILED);
                paymentTransferRepository.save(saved);
                throw exception;
            }
        } else {
            saved.setStatus(PaymentStatus.COMPLETED);
            paymentTransferRepository.save(saved);
            log.info("Legal-entity transfer completed without cashback (disabled) transferId={}", saved.getId());
        }

        return paymentMapper.toResponse(saved);
    }
}
