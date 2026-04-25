package com.rv.ecommerce.notification;

import com.rv.ecommerce.entities.PaymentTransfer;
import com.rv.ecommerce.requests.IndividualTransferRequest;
import com.rv.ecommerce.requests.LegalEntityTransferRequest;
import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentNotificationPublisher {

    private final KafkaTemplate<String, BusinessNotificationEvent> kafkaTemplate;
    private final String topic;

    public PaymentNotificationPublisher(
            @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, BusinessNotificationEvent> kafkaTemplate,
            @Value("${notification.kafka-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public BusinessNotificationEvent buildPaymentCompletedEvent(PaymentTransfer saved, IndividualTransferRequest request) {
        return buildPaymentCompletedEvent(saved, request.senderEmail(), request.recipientEmail());
    }

    public BusinessNotificationEvent buildPaymentCompletedEvent(PaymentTransfer saved, LegalEntityTransferRequest request) {
        return buildPaymentCompletedEvent(saved, request.senderEmail(), request.recipientEmail());
    }

    private static BusinessNotificationEvent buildPaymentCompletedEvent(
            PaymentTransfer saved,
            String senderEmail,
            String recipientEmail
    ) {
        Map<String, String> payload = Map.ofEntries(
                Map.entry("transferId", saved.getId().toString()),
                Map.entry("fromAccountNumber", saved.getFromAccountNumber()),
                Map.entry("toAccountNumber", saved.getToAccountNumber()),
                Map.entry("amount", saved.getAmount().toPlainString()),
                Map.entry("currency", saved.getCurrency().name()),
                Map.entry("senderEmail", Optional.ofNullable(senderEmail).orElse("")),
                Map.entry("recipientEmail", Optional.ofNullable(recipientEmail).orElse("")),
                Map.entry("senderNote", ""),
                Map.entry("recipientNote", "")
        );
        return new BusinessNotificationEvent(
                UUID.randomUUID(),
                NotificationEventType.PAYMENT_COMPLETED,
                Instant.now(),
                payload
        );
    }

    /**
     * Used by {@link com.rv.ecommerce.outbox.NotificationOutboxPublisher} after reading persisted outbox rows.
     */
    public void publish(BusinessNotificationEvent event) {
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), event).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Kafka send failed for notification event " + event.eventId(), e);
        }
    }
}
