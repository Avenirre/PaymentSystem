package com.rv.ecommerce.notification.config;

import com.rv.notification.events.BusinessNotificationEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class NotificationKafkaConfiguration {

    @Bean
    public ConsumerFactory<String, BusinessNotificationEvent> notificationConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        JacksonJsonDeserializer<BusinessNotificationEvent> deserializer =
                new JacksonJsonDeserializer<>(BusinessNotificationEvent.class, false);
        deserializer.addTrustedPackages("com.rv.notification.events", "java.util", "java.time");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BusinessNotificationEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, BusinessNotificationEvent> notificationConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, BusinessNotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory);
        return factory;
    }
}
