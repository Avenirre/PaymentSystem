package com.rv.ecommerce.mappers;

import com.rv.ecommerce.entities.AccountEntity;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountEntity toEntity(AccountRequest request, String accountNumber) {
        return AccountEntity.builder()
                .accountNumber(accountNumber)
                .ownerId(request.ownerId())
                .currency(request.currency())
                .build();
    }

    public AccountResponse toResponse(AccountEntity entity) {
        return AccountResponse.builder()
                .accountNumber(entity.getAccountNumber())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .ownerId(entity.getOwnerId())
                .balance(entity.getBalance())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
