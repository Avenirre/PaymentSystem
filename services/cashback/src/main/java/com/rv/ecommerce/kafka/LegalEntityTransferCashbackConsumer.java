package com.rv.ecommerce.kafka;

import com.rv.ecommerce.services.CashbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegalEntityTransferCashbackConsumer {

    private final CashbackService cashbackService;

    @KafkaListener(
            topics = "${cashback.kafka.legal-entity-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "legalEntityKafkaListenerContainerFactory"
    )
    public void onLegalEntityTransfer(CashbackTransferPayload payload) {
        log.info("Legal-entity transfer cashback event transferId={}", payload.transferId());
        cashbackService.processLegalEntityTransfer(payload);
    }
}
