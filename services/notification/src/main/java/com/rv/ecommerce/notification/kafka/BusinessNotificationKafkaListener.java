package com.rv.ecommerce.notification.kafka;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.rv.ecommerce.notification.dispatch.NotificationDispatchService;
import com.rv.notification.events.BusinessNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessNotificationKafkaListener {

    private final NotificationDispatchService dispatchService;
    private final JsonMapper jsonMapper;

    @KafkaListener(
            topics = "${notification.kafka-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBusinessEvent(String payload) {
        try {
            BusinessNotificationEvent event = jsonMapper.readValue(payload, BusinessNotificationEvent.class);
            log.debug("Notification event id={} type={}", event.eventId(), event.eventType());
            dispatchService.dispatch(event);
        } catch (JacksonException e) {
            log.error(
                    "Failed to parse business notification event (length={}): {}",
                    payload != null ? payload.length() : 0,
                    e.getMessage(),
                    e
            );
            throw new IllegalStateException("Invalid business notification JSON", e);
        }
    }
}
