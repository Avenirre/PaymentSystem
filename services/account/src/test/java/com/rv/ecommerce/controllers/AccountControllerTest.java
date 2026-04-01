package com.rv.ecommerce.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rv.ecommerce.exceptions.AccountCreationException;
import com.rv.ecommerce.exceptions.AccountNotFoundException;
import com.rv.ecommerce.exceptions.GlobalExceptionHandler;
import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import com.rv.ecommerce.services.AccountService;
import com.rv.ecommerce.test.JacksonTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.rv.ecommerce.entities.AccountEntity.AccountStatus;
import static com.rv.ecommerce.entities.AccountEntity.CurrencyCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import({GlobalExceptionHandler.class, JacksonTestConfiguration.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void createAccount_success_returns201() throws Exception {
        UUID ownerId = UUID.randomUUID();
        AccountRequest request = AccountRequest.builder()
                .ownerId(ownerId)
                .currency(CurrencyCode.RUB)
                .build();
        Instant now = Instant.now();
        AccountResponse response = AccountResponse.builder()
                .accountNumber("num1")
                .currency(CurrencyCode.RUB)
                .status(AccountStatus.ACTIVE)
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("num1"))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));
    }

    @Test
    void createAccount_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("currency", "RUB"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getByAccountNumber_success_returns200() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        AccountResponse response = AccountResponse.builder()
                .accountNumber("acc2")
                .currency(CurrencyCode.EUR)
                .status(AccountStatus.ACTIVE)
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(accountService.getByAccountNumber("acc2")).thenReturn(response);

        mockMvc.perform(get("/api/v1/account/acc2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("acc2"));
    }

    @Test
    void getByAccountNumber_notFound_returns404() throws Exception {
        when(accountService.getByAccountNumber("none"))
                .thenThrow(new AccountNotFoundException("none"));

        mockMvc.perform(get("/api/v1/account/none").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void createAccount_serviceFailure_returns500() throws Exception {
        UUID ownerId = UUID.randomUUID();
        AccountRequest request = AccountRequest.builder()
                .ownerId(ownerId)
                .currency(CurrencyCode.RUB)
                .build();

        doThrow(new AccountCreationException("Failed to create account", new RuntimeException("db")))
                .when(accountService).createAccount(any(AccountRequest.class));

        mockMvc.perform(post("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CREATION_FAILED"));
    }
}
