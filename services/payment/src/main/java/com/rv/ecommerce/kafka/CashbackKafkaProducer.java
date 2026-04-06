package com.rv.ecommerce.kafka;

import com.rv.ecommerce.exceptions.CashbackServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashbackKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${cashback.kafka.legal-entity-topic}")
    private String legalEntityTopic;

    @Value("${cashback.kafka.individual-topic}")
    private String individualTopic;

    public void publishLegalEntityTransfer(CashbackTransferPayload payload) {
        try {
            kafkaTemplate.send(legalEntityTopic, payload.transferId().toString(), payload)
                    .get(30, TimeUnit.SECONDS);
            log.debug("Published legal-entity cashback event topic={} transferId={}", legalEntityTopic, payload.transferId());
        } catch (Exception e) {
            throw new CashbackServiceException("Failed to send cashback event to Kafka", e);
        }
    }

    public void publishIndividualTransfer(IndividualCashbackPayload payload) {
        try {
            kafkaTemplate.send(individualTopic, payload.transferId().toString(), payload)
                    .get(30, TimeUnit.SECONDS);
            log.debug("Published individual cashback event topic={} transferId={}", individualTopic, payload.transferId());
        } catch (Exception e) {
            throw new CashbackServiceException("Failed to send individual cashback event to Kafka", e);
        }
    }
}
