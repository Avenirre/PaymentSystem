package com.rv.ecommerce.notification.kafka;

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

    @KafkaListener(
            topics = "${notification.kafka-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBusinessEvent(BusinessNotificationEvent event) {
        log.debug("Notification event id={} type={}", event.eventId(), event.eventType());
        dispatchService.dispatch(event);
    }
}
