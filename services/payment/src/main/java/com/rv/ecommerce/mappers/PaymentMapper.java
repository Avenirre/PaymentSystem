package com.rv.ecommerce.mappers;

import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentTransferResponse toResponse(PaymentTransfer entity) {
        return PaymentTransferResponse.builder()
                .transferId(entity.getId())
                .transferType(entity.getTransferType())
                .status(entity.getStatus())
                .fromAccountNumber(entity.getFromAccountNumber())
                .toAccountNumber(entity.getToAccountNumber())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .cashbackNotified(entity.isCashbackNotified())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
