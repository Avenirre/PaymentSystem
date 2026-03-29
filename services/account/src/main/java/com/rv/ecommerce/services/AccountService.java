package com.rv.ecommerce.services;

import com.rv.ecommerce.entities.AccountEntity;
import com.rv.ecommerce.repositories.AccountRepository;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse create(AccountRequest request) {
        AccountEntity entity = new AccountEntity();
        entity.setAccountNumber(generateAccountNumber());
        entity.setOwnerId(request.ownerId());
        entity.setCurrency(request.currency());

        AccountEntity saved = accountRepository.save(entity);
        return toResponse(saved);
    }

    public AccountResponse getByAccountNumber(String accountNumber) {
        AccountEntity entity = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found: " + accountNumber
                ));
        return toResponse(entity);
    }

    private AccountResponse toResponse(AccountEntity entity) {
        return new AccountResponse(
                entity.getAccountNumber(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getOwnerId(),
                entity.getBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = UUID.randomUUID().toString().replace("-", "");
        } while (accountRepository.existsById(accountNumber));
        return accountNumber;
    }
}
