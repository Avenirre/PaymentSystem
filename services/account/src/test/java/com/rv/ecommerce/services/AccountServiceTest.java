package com.rv.ecommerce.services;

import com.rv.ecommerce.entities.AccountEntity;
import com.rv.ecommerce.exceptions.AccountCreationException;
import com.rv.ecommerce.exceptions.AccountNotFoundException;
import com.rv.ecommerce.mappers.AccountMapper;
import com.rv.ecommerce.repositories.AccountRepository;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.rv.ecommerce.entities.AccountEntity.AccountStatus;
import static com.rv.ecommerce.entities.AccountEntity.CurrencyCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_success() {
        UUID ownerId = UUID.randomUUID();
        AccountRequest request = AccountRequest.builder()
                .ownerId(ownerId)
                .currency(CurrencyCode.RUB)
                .build();

        AccountEntity entity = AccountEntity.builder()
                .accountNumber("pending")
                .ownerId(ownerId)
                .currency(CurrencyCode.RUB)
                .build();

        Instant now = Instant.now();
        AccountEntity saved = AccountEntity.builder()
                .accountNumber("abc123def")
                .ownerId(ownerId)
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        AccountResponse response = AccountResponse.builder()
                .accountNumber("abc123def")
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(accountRepository.existsById(anyString())).thenReturn(false);
        when(accountMapper.toEntity(eq(request), anyString())).thenReturn(entity);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(saved);
        when(accountMapper.toResponse(saved)).thenReturn(response);

        AccountResponse result = accountService.createAccount(request);

        assertThat(result).isEqualTo(response);
        verify(accountRepository).save(entity);
    }

    @Test
    void createAccount_whenSaveFails_wrapsInAccountCreationException() {
        UUID ownerId = UUID.randomUUID();
        AccountRequest request = AccountRequest.builder()
                .ownerId(ownerId)
                .currency(CurrencyCode.EUR)
                .build();
        AccountEntity entity = AccountEntity.builder()
                .accountNumber("x")
                .ownerId(ownerId)
                .currency(CurrencyCode.EUR)
                .build();

        when(accountRepository.existsById(anyString())).thenReturn(false);
        when(accountMapper.toEntity(eq(request), anyString())).thenReturn(entity);
        when(accountRepository.save(any(AccountEntity.class))).thenThrow(new IllegalStateException("db error"));

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(AccountCreationException.class)
                .hasMessageContaining("Failed to create account")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void getByAccountNumber_success() {
        String accountNumber = "accnum1";
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        AccountEntity entity = AccountEntity.builder()
                .accountNumber(accountNumber)
                .ownerId(ownerId)
                .currency(CurrencyCode.USD)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        AccountResponse response = AccountResponse.builder()
                .accountNumber(accountNumber)
                .currency(CurrencyCode.USD)
                .status(AccountStatus.ACTIVE)
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(accountRepository.findById(accountNumber)).thenReturn(Optional.of(entity));
        when(accountMapper.toResponse(entity)).thenReturn(response);

        assertThat(accountService.getByAccountNumber(accountNumber)).isEqualTo(response);
    }

    @Test
    void getByAccountNumber_whenMissing_throwsAccountNotFoundException() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getByAccountNumber("missing"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("missing");
    }
}
