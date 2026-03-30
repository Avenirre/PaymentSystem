package com.rv.ecommerce.services;

import com.rv.ecommerce.entities.AccountEntity;
import com.rv.ecommerce.exceptions.AccountCreationException;
import com.rv.ecommerce.exceptions.AccountNotFoundException;
import com.rv.ecommerce.repositories.AccountRepository;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(AccountRequest request) {
        log.info("Creating account for ownerId={}", request.ownerId());
        try {
            AccountResponse response = toResponse(accountRepository.save(toEntity(request)));
            log.info("Account created successfully: accountNumber={}, ownerId={}", response.accountNumber(), response.ownerId());
            return response;
        } catch (Exception exception) {
            log.error("Failed to create account for ownerId={}", request.ownerId(), exception);
            throw new AccountCreationException("Failed to create account", exception);
        }
    }

    public AccountResponse getByAccountNumber(String accountNumber) {
        log.info("Fetching account by accountNumber={}", accountNumber);
        AccountEntity entity = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        AccountResponse response = toResponse(entity);
        log.info("Account fetched successfully: accountNumber={}, status={}", response.accountNumber(), response.status());
        return response;
    }

    private AccountResponse toResponse(AccountEntity entity) {
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

    private AccountEntity toEntity(AccountRequest request) {
        String generatedAccountNumber = generateAccountNumber();
        log.debug("Generated accountNumber={}", generatedAccountNumber);
        return AccountEntity.builder()
                .accountNumber(generatedAccountNumber)
                .ownerId(request.ownerId())
                .currency(request.currency())
                .build();
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = UUID.randomUUID().toString().replace("-", "");
        } while (accountRepository.existsById(accountNumber));
        return accountNumber;
    }
}
