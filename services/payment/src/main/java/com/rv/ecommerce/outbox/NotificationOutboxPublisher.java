package com.rv.ecommerce.outbox;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.rv.ecommerce.entities.NotificationOutbox;
import com.rv.ecommerce.entities.NotificationOutboxStatus;
import com.rv.ecommerce.notification.PaymentNotificationPublisher;
import com.rv.ecommerce.repositories.NotificationOutboxRepository;
import com.rv.notification.events.BusinessNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publishes {@link com.rv.ecommerce.entities.NotificationOutbox} rows to Kafka in a separate transaction
 * from {@link com.rv.ecommerce.services.PaymentService} transfer commits (transactional outbox).
 */
@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final PaymentNotificationPublisher paymentNotificationPublisher;
    private final JsonMapper jsonMapper;

    @Value("${notification.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${notification.outbox.publish-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<NotificationOutbox> rows = notificationOutboxRepository.lockPendingBatch(batchSize);
        if (rows.isEmpty()) {
            return;
        }
        rows.forEach(this::publishRow);
    }

    private void publishRow(NotificationOutbox row) {
        BusinessNotificationEvent event = deserialize(row);
        paymentNotificationPublisher.publish(event);
        row.setStatus(NotificationOutboxStatus.PUBLISHED);
        row.setPublishedAt(Instant.now());
    }

    private BusinessNotificationEvent deserialize(NotificationOutbox row) {
        try {
            return jsonMapper.readValue(row.getPayloadJson(), BusinessNotificationEvent.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid notification outbox payload id=" + row.getId(), e);
        }
    }
}
