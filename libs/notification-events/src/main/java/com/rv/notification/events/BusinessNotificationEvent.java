package com.rv.notification.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka value for topic {@code business.notification.events}. All payload values are strings for Mustache rendering.
 */
public record BusinessNotificationEvent(
        UUID eventId,
        NotificationEventType eventType,
        Instant occurredAt,
        Map<String, String> payload
) {
}
