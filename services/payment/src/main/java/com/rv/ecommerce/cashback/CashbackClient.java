package com.rv.ecommerce.cashback;

import com.rv.ecommerce.exceptions.CashbackServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class CashbackClient {

    private final RestClient cashbackRestClient;

    @Value("${cashback.notify-path}")
    private String notifyPath;

    public void notifyLegalEntityTransfer(CashbackTransferPayload payload) {
        try {
//            cashbackRestClient.post()
//                    .uri(notifyPath)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(payload)
//                    .retrieve()
//                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new CashbackServiceException("Cashback service call failed", exception);
        }
    }
}
