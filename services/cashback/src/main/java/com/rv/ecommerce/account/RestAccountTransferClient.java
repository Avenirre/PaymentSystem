package com.rv.ecommerce.account;

import com.rv.ecommerce.config.CashbackSettlementProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestAccountTransferClient implements AccountTransferClient {

    private final RestClient restClient;

    public RestAccountTransferClient(CashbackSettlementProperties settlement) {
        this.restClient = RestClient.builder()
                .baseUrl(settlement.accountBaseUrl().replaceAll("/$", ""))
                .build();
    }

    @Override
    public void applyTransfer(AccountApplyTransferRequest request) {
        restClient.post()
                .uri("/api/v1/account/transfers/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
