package com.rv.ecommerce.controllers;

import com.rv.ecommerce.entities.CashbackType;
import com.rv.ecommerce.requests.UpdateCashbackRequest;
import com.rv.ecommerce.responses.CashbackResponse;
import com.rv.ecommerce.services.CashbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cashback/rates")
@Tag(name = "Cashback rates", description = "Percent from DB when set, otherwise from configuration")
@RequiredArgsConstructor
public class CashbackController {

    private final CashbackService cashbackService;

    @GetMapping("/individual")
    @Operation(summary = "Get effective cashback percent for individuals")
    public CashbackResponse getIndividualPercent() {
        return new CashbackResponse(cashbackService.getEffectivePercent(CashbackType.INDIVIDUAL));
    }

    @PutMapping("/individual")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Persist cashback percent for individuals (MongoDB)")
    public void putIndividualPercent(@Valid @RequestBody UpdateCashbackRequest request) {
        cashbackService.updatePercent(CashbackType.INDIVIDUAL, request.percent());
    }

    @GetMapping("/legal-entity")
    @Operation(summary = "Get effective cashback percent for legal entities")
    public CashbackResponse getLegalEntityPercent() {
        return new CashbackResponse(cashbackService.getEffectivePercent(CashbackType.LEGAL_ENTITY));
    }

    @PutMapping("/legal-entity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Persist cashback percent for legal entities (MongoDB)")
    public void putLegalEntityPercent(@Valid @RequestBody UpdateCashbackRequest request) {
        cashbackService.updatePercent(CashbackType.LEGAL_ENTITY, request.percent());
    }
}
