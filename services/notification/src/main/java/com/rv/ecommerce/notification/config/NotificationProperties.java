package com.rv.ecommerce.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        /** Kafka topic for {@link com.rv.notification.events.BusinessNotificationEvent}. */
        String kafkaTopic,
        /**
         * If set, all messages are sent to this address (testing).
         */
        String forceToAddress
) {
}
