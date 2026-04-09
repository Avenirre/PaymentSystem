package com.rv.ecommerce.outbox;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.rv.ecommerce.entities.CashbackOutbox;
import com.rv.ecommerce.entities.CashbackOutboxStatus;
import com.rv.ecommerce.kafka.CashbackTransferPayload;
import com.rv.ecommerce.kafka.IndividualCashbackPayload;
import com.rv.ecommerce.repositories.CashbackOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays {@link CashbackOutbox} rows to Kafka.
 * <p>
 * Uses a single JPA {@link Transactional} boundary: synchronous {@link KafkaTemplate#send} + flush
 * before marking rows {@link CashbackOutboxStatus#PUBLISHED}. On send failure the transaction rolls back
 * (rows stay PENDING). {@code ChainedTransactionManager} is not used — it was removed in Spring Framework 6.2;
 * cashback consumer idempotency covers rare duplicate delivery if Kafka committed before DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CashbackOutboxPublisher {

    private final CashbackOutboxRepository cashbackOutboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${cashback.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${cashback.outbox.publish-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<CashbackOutbox> rows = cashbackOutboxRepository.lockPendingBatch(batchSize);
        if (rows.isEmpty()) {
            return;
        }
        for (CashbackOutbox row : rows) {
            Object payload = deserialize(row);
            try {
                kafkaTemplate.send(row.getTopic(), row.getPartitionKey(), payload)
                        .get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("Kafka send failed for outbox id=" + row.getId(), e);
            }
            row.setStatus(CashbackOutboxStatus.PUBLISHED);
            row.setPublishedAt(Instant.now());
        }
    }

    private Object deserialize(CashbackOutbox row) {
        try {
            return switch (row.getEventType()) {
                case LEGAL_ENTITY_CASHBACK -> jsonMapper.readValue(
                        row.getPayloadJson(), CashbackTransferPayload.class);
                case INDIVIDUAL_CASHBACK -> jsonMapper.readValue(
                        row.getPayloadJson(), IndividualCashbackPayload.class);
            };
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid outbox payload id=" + row.getId(), e);
        }
    }
}
