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
import com.rv.ecommerce.notification.BusinessNotificationPublisher;
import com.rv.ecommerce.repositories.AccountRepository;
import com.rv.ecommerce.repositories.AccountTransferLedgerRepository;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.requests.ApplyTransferRequest;
import com.rv.ecommerce.responses.AccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransferLedgerRepository accountTransferLedgerRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private BusinessNotificationPublisher businessNotificationPublisher;

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
        verify(businessNotificationPublisher).publishAccountCreated(response, request);
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

        verify(businessNotificationPublisher, never()).publishAccountCreated(any(), any());
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

    @Test
    void applyTransfer_movesBalancesAndWritesLedger() {
        UUID transferId = UUID.randomUUID();
        AccountEntity from = AccountEntity.builder()
                .accountNumber("from")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        AccountEntity to = AccountEntity.builder()
                .accountNumber("to")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.empty());
        when(accountRepository.findById("from")).thenReturn(Optional.of(from));
        when(accountRepository.findById("to")).thenReturn(Optional.of(to));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplyTransferRequest request = ApplyTransferRequest.builder()
                .transferId(transferId)
                .fromAccountNumber("from")
                .toAccountNumber("to")
                .amount(new BigDecimal("30.00"))
                .currency(CurrencyCode.RUB)
                .build();

        accountService.applyTransfer(request);

        verify(accountRepository, times(2)).save(any(AccountEntity.class));
        assertThat(from.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("30.00"));

        ArgumentCaptor<AccountTransferLedger> ledgerCaptor = ArgumentCaptor.forClass(AccountTransferLedger.class);
        verify(accountTransferLedgerRepository).save(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getState()).isEqualTo(LedgerState.APPLIED);
        assertThat(ledgerCaptor.getValue().getTransferId()).isEqualTo(transferId);
    }

    @Test
    void applyTransfer_whenAlreadyApplied_isIdempotent() {
        UUID transferId = UUID.randomUUID();
        AccountTransferLedger ledger = AccountTransferLedger.builder()
                .transferId(transferId)
                .state(LedgerState.APPLIED)
                .build();
        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.of(ledger));

        ApplyTransferRequest request = ApplyTransferRequest.builder()
                .transferId(transferId)
                .fromAccountNumber("from")
                .toAccountNumber("to")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .build();

        accountService.applyTransfer(request);

        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void applyTransfer_whenInsufficientFunds_throws() {
        UUID transferId = UUID.randomUUID();
        AccountEntity from = AccountEntity.builder()
                .accountNumber("from")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("5.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        AccountEntity to = AccountEntity.builder()
                .accountNumber("to")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.empty());
        when(accountRepository.findById("from")).thenReturn(Optional.of(from));
        when(accountRepository.findById("to")).thenReturn(Optional.of(to));

        ApplyTransferRequest request = ApplyTransferRequest.builder()
                .transferId(transferId)
                .fromAccountNumber("from")
                .toAccountNumber("to")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .build();

        assertThatThrownBy(() -> accountService.applyTransfer(request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void applyTransfer_whenCompensated_throwsConflict() {
        UUID transferId = UUID.randomUUID();
        AccountTransferLedger ledger = AccountTransferLedger.builder()
                .transferId(transferId)
                .state(LedgerState.COMPENSATED)
                .build();
        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.of(ledger));

        ApplyTransferRequest request = ApplyTransferRequest.builder()
                .transferId(transferId)
                .fromAccountNumber("from")
                .toAccountNumber("to")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .build();

        assertThatThrownBy(() -> accountService.applyTransfer(request))
                .isInstanceOf(AccountTransferConflictException.class);
    }

    @Test
    void compensateTransfer_reversesAppliedTransfer() {
        UUID transferId = UUID.randomUUID();
        AccountTransferLedger ledger = AccountTransferLedger.builder()
                .transferId(transferId)
                .fromAccountNumber("from")
                .toAccountNumber("to")
                .amount(new BigDecimal("25.00"))
                .currency(CurrencyCode.RUB)
                .state(LedgerState.APPLIED)
                .build();

        AccountEntity from = AccountEntity.builder()
                .accountNumber("from")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("75.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        AccountEntity to = AccountEntity.builder()
                .accountNumber("to")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("25.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.of(ledger));
        when(accountRepository.findById("from")).thenReturn(Optional.of(from));
        when(accountRepository.findById("to")).thenReturn(Optional.of(to));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountTransferLedgerRepository.save(any(AccountTransferLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.compensateTransfer(transferId);

        assertThat(from.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(to.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.getState()).isEqualTo(LedgerState.COMPENSATED);
    }

    @Test
    void compensateTransfer_unknownTransfer_noop() {
        UUID transferId = UUID.randomUUID();
        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.empty());

        accountService.compensateTransfer(transferId);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void applyTransfer_sameFromAndTo_throwsInvalidTransfer() {
        UUID transferId = UUID.randomUUID();
        AccountEntity acc = AccountEntity.builder()
                .accountNumber("same")
                .ownerId(UUID.randomUUID())
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(accountTransferLedgerRepository.findById(transferId)).thenReturn(Optional.empty());
        when(accountRepository.findById("same")).thenReturn(Optional.of(acc));

        ApplyTransferRequest request = ApplyTransferRequest.builder()
                .transferId(transferId)
                .fromAccountNumber("same")
                .toAccountNumber("same")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .build();

        assertThatThrownBy(() -> accountService.applyTransfer(request))
                .isInstanceOf(InvalidTransferException.class);
    }
}
