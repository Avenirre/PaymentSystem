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

    public void publishPaymentCompleted(PaymentTransfer saved, IndividualTransferRequest request) {
        publishPaymentCompleted(saved,
                request.senderEmail(),
                request.recipientEmail());
    }

    public void publishPaymentCompleted(PaymentTransfer saved, LegalEntityTransferRequest request) {
        publishPaymentCompleted(saved,
                request.senderEmail(),
                request.recipientEmail());
    }

    private void publishPaymentCompleted(PaymentTransfer saved, String senderEmail, String recipientEmail) {
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
        BusinessNotificationEvent event = new BusinessNotificationEvent(
                UUID.randomUUID(),
                NotificationEventType.PAYMENT_COMPLETED,
                Instant.now(),
                payload
        );
        kafkaTemplate.send(topic, event.eventId().toString(), event);
    }
}
