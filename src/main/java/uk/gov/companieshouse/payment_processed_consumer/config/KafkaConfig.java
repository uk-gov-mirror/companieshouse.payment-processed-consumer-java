package uk.gov.companieshouse.payment_processed_consumer.config;

import consumer.deserialization.AvroDeserializer;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import payments.payment_processed;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.payment_processed_consumer.PaymentProcessedConsumerApplication;



@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, payment_processed> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, AvroDeserializer.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new AvroDeserializer<>(payment_processed.class)));
    }



    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, payment_processed>
    kafkaListenerContainerFactory(ConsumerFactory<String, payment_processed> consumerFactory,
            @Value("${consumer.concurrency}") Integer concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, payment_processed> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    Logger getLogger() {
        return LoggerFactory.getLogger(PaymentProcessedConsumerApplication.APPLICATION_NAME_SPACE);
    }
}

