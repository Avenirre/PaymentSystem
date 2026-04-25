package com.rv.ecommerce.notification;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.rv.ecommerce.entities.NotificationOutbox;
import com.rv.ecommerce.entities.NotificationOutboxEventType;
import com.rv.ecommerce.entities.NotificationOutboxStatus;
import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.repositories.NotificationOutboxRepository;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.notification.events.BusinessNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationOutboxWriter {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final PaymentNotificationPublisher paymentNotificationPublisher;
    private final JsonMapper jsonMapper;

    @Value("${notification.kafka-topic}")
    private String notificationTopic;

    public void enqueuePaymentCompleted(PaymentTransfer saved, IndividualTransferRequest request) {
        saveRow(saved, paymentNotificationPublisher.buildPaymentCompletedEvent(saved, request));
    }

    public void enqueuePaymentCompleted(PaymentTransfer saved, LegalEntityTransferRequest request) {
        saveRow(saved, paymentNotificationPublisher.buildPaymentCompletedEvent(saved, request));
    }

    private void saveRow(PaymentTransfer saved, BusinessNotificationEvent event) {
        String payloadJson;
        try {
            payloadJson = jsonMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize notification outbox payload transferId=" + saved.getId(), e);
        }
        notificationOutboxRepository.save(NotificationOutbox.builder()
                .id(UUID.randomUUID())
                .aggregateId(saved.getId())
                .eventType(NotificationOutboxEventType.PAYMENT_COMPLETED)
                .topic(notificationTopic)
                .partitionKey(saved.getId().toString())
                .payloadJson(payloadJson)
                .status(NotificationOutboxStatus.PENDING)
                .createdAt(Instant.now())
                .attemptCount(0)
                .build());
    }
}
