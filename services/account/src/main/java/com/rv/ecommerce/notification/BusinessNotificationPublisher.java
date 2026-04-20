package com.rv.ecommerce.notification;

import com.rv.ecommerce.requests.AccountRequest;
import com.rv.ecommerce.responses.AccountResponse;
import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BusinessNotificationPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${notification.kafka-topic}")
    private String topic;

    public void publishAccountCreated(AccountResponse response, AccountRequest request) {
        Map<String, String> payload = Map.ofEntries(
                Map.entry("accountNumber", response.accountNumber()),
                Map.entry("ownerId", response.ownerId().toString()),
                Map.entry("currency", response.currency().name()),
                Map.entry("ownerEmail", Optional.ofNullable(request.ownerEmail()).orElse(""))
        );
        BusinessNotificationEvent event = new BusinessNotificationEvent(
                UUID.randomUUID(),
                NotificationEventType.ACCOUNT_CREATED,
                Instant.now(),
                payload
        );
        kafkaTemplate.send(topic, event.eventId().toString(), event);
    }
}
