package com.rv.ecommerce.controllers;

import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.requests.ApplyTransferRequest;
import com.rv.ecommerce.requests.CompensateTransferRequest;
import com.rv.ecommerce.responses.AccountResponse;
import com.rv.ecommerce.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Accounts", description = "Operations for bank accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create account", description = "Creates a new account and returns its data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "500", description = "Account creation failed")
    })
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account by number", description = "Returns account by account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public AccountResponse getAccountByAccountNumber(@PathVariable String accountNumber) {
        return accountService.getByAccountNumber(accountNumber);
    }

    @PostMapping("/transfers/apply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Apply transfer", description = "Moves funds between accounts (idempotent by transferId)")
    public void applyTransfer(@Valid @RequestBody ApplyTransferRequest request) {
        accountService.applyTransfer(request);
    }

    @PostMapping("/transfers/compensate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Compensate transfer", description = "Reverses a previously applied transfer (idempotent)")
    public void compensateTransfer(@Valid @RequestBody CompensateTransferRequest request) {
        accountService.compensateTransfer(request.transferId());
    }
}
