package com.rv.ecommerce.controllers;

import tools.jackson.databind.json.JsonMapper;
import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import com.rv.ecommerce.entities.PaymentTransfer.PaymentStatus;
import com.rv.ecommerce.entities.PaymentTransfer.TransferType;
import com.rv.ecommerce.exceptions.CashbackServiceException;
import com.rv.ecommerce.exceptions.GlobalExceptionHandler;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.ecommerce.responses.PaymentTransferResponse;
import com.rv.ecommerce.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void transferIndividual_success_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        IndividualTransferRequest request = IndividualTransferRequest.builder()
                .fromAccountNumber("a1")
                .toAccountNumber("b1")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.INDIVIDUAL)
                .status(PaymentStatus.COMPLETED)
                .fromAccountNumber("a1")
                .toAccountNumber("b1")
                .amount(new BigDecimal("10.00"))
                .currency(CurrencyCode.RUB)
                .cashbackNotified(false)
                .createdAt(now)
                .build();

        when(paymentService.transferToIndividual(any(IndividualTransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/transfers/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value(id.toString()))
                .andExpect(jsonPath("$.fromAccountNumber").value("a1"));
    }

    @Test
    void transferIndividual_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments/transfers/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("currency", "RUB"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transferLegalEntity_success_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("x")
                .toAccountNumber("y")
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyCode.RUB)
                .legalEntityInn("1234567890")
                .legalEntityName("OOO Roga")
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        PaymentTransferResponse response = PaymentTransferResponse.builder()
                .transferId(id)
                .transferType(TransferType.LEGAL_ENTITY)
                .status(PaymentStatus.COMPLETED)
                .fromAccountNumber("x")
                .toAccountNumber("y")
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyCode.RUB)
                .cashbackNotified(true)
                .createdAt(now)
                .build();

        when(paymentService.transferToLegalEntity(any(LegalEntityTransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/transfers/legal-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value(id.toString()))
                .andExpect(jsonPath("$.cashbackNotified").value(true));
    }

    @Test
    void transferLegalEntity_cashbackError_returns502() throws Exception {
        LegalEntityTransferRequest request = LegalEntityTransferRequest.builder()
                .fromAccountNumber("x")
                .toAccountNumber("y")
                .amount(BigDecimal.TEN)
                .currency(CurrencyCode.RUB)
                .legalEntityInn("1234567890")
                .legalEntityName("OOO")
                .senderEmail(null)
                .recipientEmail(null)
                .build();

        doThrow(new CashbackServiceException("Cashback service call failed", new RuntimeException("down")))
                .when(paymentService).transferToLegalEntity(any(LegalEntityTransferRequest.class));

        mockMvc.perform(post("/api/v1/payments/transfers/legal-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CASHBACK_SERVICE_ERROR"));
    }
}
