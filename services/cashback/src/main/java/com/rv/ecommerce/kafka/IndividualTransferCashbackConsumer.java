package com.rv.ecommerce.kafka;

import com.rv.ecommerce.services.CashbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndividualTransferCashbackConsumer {

    private final CashbackService cashbackService;

    @KafkaListener(
            topics = "${cashback.kafka.individual-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "individualKafkaListenerContainerFactory"
    )
    public void onIndividualTransfer(IndividualCashbackPayload payload) {
        log.info("Individual transfer cashback event transferId={}", payload.transferId());
        cashbackService.processIndividualTransfer(payload);
    }
}
