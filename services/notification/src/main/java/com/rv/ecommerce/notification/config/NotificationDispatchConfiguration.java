package com.rv.ecommerce.notification.config;

import com.rv.ecommerce.notification.dispatch.NotificationEventDispatchStrategy;
import com.rv.notification.events.NotificationEventType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class NotificationDispatchConfiguration {

    @Bean
    Map<NotificationEventType, NotificationEventDispatchStrategy> notificationDispatchStrategies(
            List<NotificationEventDispatchStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        NotificationEventDispatchStrategy::eventType,
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate dispatch strategy for " + existing.eventType());
                        },
                        () -> new EnumMap<>(NotificationEventType.class)));
    }
}
