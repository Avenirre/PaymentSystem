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

    @Value("${cashback.kafka.topic}")
    private String topic;

    public void publishLegalEntityTransfer(CashbackTransferPayload payload) {
        try {
            kafkaTemplate.send(topic, payload.transferId().toString(), payload)
                    .get(30, TimeUnit.SECONDS);
            log.debug("Published cashback event to Kafka topic={} transferId={}", topic, payload.transferId());
        } catch (Exception e) {
            throw new CashbackServiceException("Failed to send cashback event to Kafka", e);
        }
    }
}
