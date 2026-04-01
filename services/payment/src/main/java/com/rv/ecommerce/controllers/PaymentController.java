package com.rv.ecommerce.controllers;

import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import com.rv.ecommerce.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/transfers")
@Tag(name = "Payments", description = "Transfers between accounts")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/individual")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer to individual", description = "P2P transfer to another individual account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer registered",
                    content = @Content(schema = @Schema(implementation = PaymentTransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public PaymentTransferResponse transferIndividual(@Valid @RequestBody IndividualTransferRequest request) {
        return paymentService.transferToIndividual(request);
    }

    @PostMapping("/legal-entity")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer to legal entity",
            description = "Transfer to a legal-entity account; notifies Cashback service when enabled")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer registered",
                    content = @Content(schema = @Schema(implementation = PaymentTransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "502", description = "Cashback service error")
    })
    public PaymentTransferResponse transferLegalEntity(@Valid @RequestBody LegalEntityTransferRequest request) {
        return paymentService.transferToLegalEntity(request);
    }
}
