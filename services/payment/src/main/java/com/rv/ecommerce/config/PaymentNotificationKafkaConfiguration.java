package com.rv.ecommerce.config;

import com.rv.notification.events.BusinessNotificationEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Non-transactional Kafka producer for business notification events (separate from transactional cashback outbox producer).
 */
@Configuration
public class PaymentNotificationKafkaConfiguration {

    @Bean
    public ProducerFactory<String, BusinessNotificationEvent> businessNotificationProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, BusinessNotificationEvent> notificationKafkaTemplate(
            ProducerFactory<String, BusinessNotificationEvent> businessNotificationProducerFactory) {
        return new KafkaTemplate<>(businessNotificationProducerFactory);
    }
}
