package com.rv.ecommerce.notification;

import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CashbackNotificationPublisher {

    private final KafkaTemplate<String, BusinessNotificationEvent> kafkaTemplate;
    private final String topic;

    public CashbackNotificationPublisher(
            @Qualifier("cashbackNotificationKafkaTemplate") KafkaTemplate<String, BusinessNotificationEvent> kafkaTemplate,
            @Value("${notification.kafka-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishMonthlyCashbackPayout(
            String yearMonth,
            String beneficiaryAccountNumber,
            String currency,
            BigDecimal amount,
            UUID payoutTransferId,
            String recipientEmail
    ) {
        Map<String, String> payload = Map.ofEntries(
                Map.entry("yearMonth", yearMonth),
                Map.entry("beneficiaryAccountNumber", beneficiaryAccountNumber),
                Map.entry("currency", currency),
                Map.entry("amount", amount.toPlainString()),
                Map.entry("payoutReference", payoutTransferId.toString()),
                Map.entry("recipientEmail", Optional.ofNullable(recipientEmail).orElse(""))
        );
        BusinessNotificationEvent event = new BusinessNotificationEvent(
                UUID.randomUUID(),
                NotificationEventType.CASHBACK_MONTHLY_PAYOUT,
                Instant.now(),
                payload
        );
        kafkaTemplate.send(topic, event.eventId().toString(), event);
    }
}
