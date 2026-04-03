package com.rv.ecommerce.account;

import com.rv.ecommerce.entities.PaymentTransfer.CurrencyCode;
import com.rv.ecommerce.exceptions.AccountOperationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AccountClient {

    private static final String APPLY_PATH = "/api/v1/account/transfers/apply";
    private static final String COMPENSATE_PATH = "/api/v1/account/transfers/compensate";

    private final RestClient accountRestClient;

    public AccountClient(@Qualifier("accountRestClient") RestClient accountRestClient) {
        this.accountRestClient = accountRestClient;
    }

    public void applyTransfer(
            UUID transferId,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            CurrencyCode currency
    ) {
        var payload = new ApplyAccountTransferPayload(transferId, fromAccountNumber, toAccountNumber, amount, currency);
        try {
            accountRestClient.post()
                    .uri(APPLY_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
                        String body = readBody(response);
                        throw new AccountOperationException(response.getStatusCode(), body);
                    })
                    .toBodilessEntity();
        } catch (AccountOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new AccountOperationException("Account service call failed", e);
        }
    }

    public void compensateTransfer(UUID transferId) {
        var payload = new CompensateAccountTransferPayload(transferId);
        try {
            accountRestClient.post()
                    .uri(COMPENSATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
                        String body = readBody(response);
                        throw new AccountOperationException(response.getStatusCode(), body);
                    })
                    .toBodilessEntity();
        } catch (AccountOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new AccountOperationException("Account compensation call failed", e);
        }
    }

    private static String readBody(ClientHttpResponse response) {
        try (InputStream in = response.getBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
