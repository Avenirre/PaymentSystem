package com.rv.ecommerce.config;

import com.rv.ecommerce.kafka.CashbackTransferPayload;
import com.rv.ecommerce.kafka.IndividualCashbackPayload;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class CashbackKafkaListenerConfiguration {

    @Bean
    public ConsumerFactory<String, CashbackTransferPayload> legalEntityTransferConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        JacksonJsonDeserializer<CashbackTransferPayload> deserializer =
                new JacksonJsonDeserializer<>(CashbackTransferPayload.class, false);
        deserializer.addTrustedPackages("com.rv.ecommerce.kafka");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CashbackTransferPayload> legalEntityKafkaListenerContainerFactory(
            ConsumerFactory<String, CashbackTransferPayload> legalEntityTransferConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, CashbackTransferPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(legalEntityTransferConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, IndividualCashbackPayload> individualTransferConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        JacksonJsonDeserializer<IndividualCashbackPayload> deserializer =
                new JacksonJsonDeserializer<>(IndividualCashbackPayload.class, false);
        deserializer.addTrustedPackages("com.rv.ecommerce.kafka");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IndividualCashbackPayload> individualKafkaListenerContainerFactory(
            ConsumerFactory<String, IndividualCashbackPayload> individualTransferConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, IndividualCashbackPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(individualTransferConsumerFactory);
        return factory;
    }
}
