package com.rv.ecommerce.controllers;

import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import com.rv.ecommerce.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
        log.info("POST /account called");
        AccountResponse response = accountService.createAccount(request);
        log.info("POST /account completed accountNumber={}", response.accountNumber());
        return response;
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccountByAccountNumber(@PathVariable String accountNumber) {
        log.info("GET /account/{} called", accountNumber);
        AccountResponse response = accountService.getByAccountNumber(accountNumber);
        log.info("GET /account/{} completed status={}", accountNumber, response.status());
        return response;
    }
}
