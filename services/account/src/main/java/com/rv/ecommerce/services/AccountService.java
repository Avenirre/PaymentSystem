package com.rv.ecommerce.services;

import com.rv.ecommerce.entities.AccountEntity;
import com.rv.ecommerce.entities.AccountTransferLedger;
import com.rv.ecommerce.entities.AccountTransferLedger.LedgerState;
import com.rv.ecommerce.exceptions.AccountNotFoundException;
import com.rv.ecommerce.exceptions.AccountTransferConflictException;
import com.rv.ecommerce.exceptions.AccountCreationException;
import com.rv.ecommerce.exceptions.InsufficientFundsException;
import com.rv.ecommerce.exceptions.InvalidTransferException;
import com.rv.ecommerce.mappers.AccountMapper;
import com.rv.ecommerce.repositories.AccountRepository;
import com.rv.ecommerce.repositories.AccountTransferLedgerRepository;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.requests.ApplyTransferRequest;
import com.rv.ecommerce.responses.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTransferLedgerRepository accountTransferLedgerRepository;
    private final AccountMapper accountMapper;

    public AccountResponse createAccount(AccountRequest request) {
        log.info("Creating account for ownerId={}", request.ownerId());
        try {
            String generatedAccountNumber = generateAccountNumber();
            log.debug("Generated accountNumber={}", generatedAccountNumber);
            AccountResponse response = accountMapper.toResponse(accountRepository.save(
                    accountMapper.toEntity(request, generatedAccountNumber)
            ));
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
        AccountResponse response = accountMapper.toResponse(entity);
        log.info("Account fetched successfully: accountNumber={}, status={}", response.accountNumber(), response.status());
        return response;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void applyTransfer(ApplyTransferRequest request) {
        UUID transferId = request.transferId();
        Optional<AccountTransferLedger> existing = accountTransferLedgerRepository.findById(transferId);
        if (existing.isPresent()) {
            if (existing.get().getState() == LedgerState.COMPENSATED) {
                throw new AccountTransferConflictException(transferId, "Transfer was already compensated");
            }
            log.info("applyTransfer idempotent transferId={}", transferId);
            return;
        }

        AccountEntity from = accountRepository.findById(request.fromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.fromAccountNumber()));
        AccountEntity to = accountRepository.findById(request.toAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.toAccountNumber()));

        validateAccountsForTransfer(from, to, request.currency(), request.amount());

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));
        accountRepository.save(from);
        accountRepository.save(to);

        accountTransferLedgerRepository.save(AccountTransferLedger.builder()
                .transferId(transferId)
                .fromAccountNumber(request.fromAccountNumber())
                .toAccountNumber(request.toAccountNumber())
                .amount(request.amount())
                .currency(request.currency())
                .state(LedgerState.APPLIED)
                .build());

        log.info("applyTransfer completed transferId={}", transferId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void compensateTransfer(UUID transferId) {
        Optional<AccountTransferLedger> ledgerOpt = accountTransferLedgerRepository.findById(transferId);
        if (ledgerOpt.isEmpty()) {
            log.info("compensateTransfer noop unknown transferId={}", transferId);
            return;
        }
        AccountTransferLedger ledger = ledgerOpt.get();
        if (ledger.getState() == LedgerState.COMPENSATED) {
            log.info("compensateTransfer idempotent transferId={}", transferId);
            return;
        }

        AccountEntity from = accountRepository.findById(ledger.getFromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(ledger.getFromAccountNumber()));
        AccountEntity to = accountRepository.findById(ledger.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(ledger.getToAccountNumber()));

        BigDecimal amount = ledger.getAmount();
        if (to.getBalance().compareTo(amount) < 0) {
            throw new InvalidTransferException("Cannot compensate: insufficient balance on receiver account");
        }

        from.setBalance(from.getBalance().add(amount));
        to.setBalance(to.getBalance().subtract(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        ledger.setState(LedgerState.COMPENSATED);
        accountTransferLedgerRepository.save(ledger);

        log.info("compensateTransfer completed transferId={}", transferId);
    }

    private void validateAccountsForTransfer(
            AccountEntity from,
            AccountEntity to,
            AccountEntity.CurrencyCode requestCurrency,
            BigDecimal amount
    ) {
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            throw new InvalidTransferException("From and to accounts must differ");
        }
        if (from.getStatus() != AccountEntity.AccountStatus.ACTIVE || to.getStatus() != AccountEntity.AccountStatus.ACTIVE) {
            throw new InvalidTransferException("Both accounts must be ACTIVE");
        }
        if (from.getCurrency() != to.getCurrency()) {
            throw new InvalidTransferException("Account currencies must match");
        }
        if (from.getCurrency() != requestCurrency) {
            throw new InvalidTransferException("Transfer currency must match account currency");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(from.getAccountNumber());
        }
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = UUID.randomUUID().toString().replace("-", "");
        } while (accountRepository.existsById(accountNumber));
        return accountNumber;
    }
}
