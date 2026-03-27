package com.usermanagement.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.usermanagement.service.dto.AuditLogEvent;

/**
 * Kafka Configuration
 * Topic definitions and producer/consumer factories
 *
 * @author Service Team
 * @since 1.0
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${app.kafka.topics.audit-logs}")
    private String auditLogsTopic;

    @Value("${app.kafka.topics.audit-logs-retry}")
    private String auditLogsRetryTopic;

    @Value("${app.kafka.topics.audit-logs-dlt}")
    private String auditLogsDltTopic;

    @Value("${app.kafka.partitions:3}")
    private int partitions;

    @Value("${app.kafka.replication-factor:1}")
    private short replicationFactor;

    /**
     * Producer factory with JSON serializer
     */
    @Bean
    public ProducerFactory<String, AuditLogEvent> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        logger.info("Creating Kafka ProducerFactory with config: bootstrap.servers={}",
                config.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka template for sending messages
     */
    @Bean
    public KafkaTemplate<String, AuditLogEvent> kafkaTemplate(
            ProducerFactory<String, AuditLogEvent> producerFactory) {
        KafkaTemplate<String, AuditLogEvent> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(auditLogsTopic);
        return template;
    }

    /**
     * Consumer factory with JSON deserializer
     */
    @Bean
    public ConsumerFactory<String, AuditLogEvent> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.usermanagement.service.dto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.usermanagement.service.dto.AuditLogEvent");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        logger.info("Creating Kafka ConsumerFactory with config: group.id={}, auto.offset.reset=earliest",
                config.get(ConsumerConfig.GROUP_ID_CONFIG));

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Kafka listener container factory with error handling
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditLogEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, AuditLogEvent> consumerFactory,
            KafkaTemplate<String, AuditLogEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, AuditLogEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Error handling with retry and DLT
        DefaultErrorHandler errorHandler = createErrorHandler(kafkaTemplate);
        factory.setCommonErrorHandler(errorHandler);

        // Concurrency settings
        factory.setConcurrency(3);

        logger.info("Created KafkaListenerContainerFactory with concurrency=3, retry=3, backoff=1000ms");

        return factory;
    }

    /**
     * Create error handler with retry and DLT
     */
    private DefaultErrorHandler createErrorHandler(KafkaTemplate<String, AuditLogEvent> kafkaTemplate) {
        // Fixed backoff: 1 second interval, 3 retries
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    // Custom DLT handler
                    logger.error("Message moved to DLT after retries exhausted: topic={}, partition={}, offset={}",
                            record.topic(), record.partition(), record.offset(), exception);
                },
                backOff
        );

        // Add retryable exceptions
        errorHandler.addRetryableExceptions(
                org.springframework.dao.DataAccessException.class,
                org.springframework.transaction.TransactionSystemException.class,
                java.sql.SQLException.class
        );

        // Add not retryable exceptions (go directly to DLT)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                com.fasterxml.jackson.core.JsonProcessingException.class
        );

        return errorHandler;
    }

    /**
     * Retry topic configuration for automatic retry and DLT
     */
    @Bean
    public RetryTopicConfiguration retryTopicConfiguration(
            KafkaTemplate<String, AuditLogEvent> kafkaTemplate) {

        return RetryTopicConfigurationBuilder
                .newInstance()
                .exponentialBackoff(1000L, 2.0, 5000L)
                .maxAttempts(4) // Initial + 3 retries
                .includeTopic(auditLogsTopic)
                .retryTopicSuffix("-retry")
                .dltSuffix("-dlt")
                .doNotAutoCreateRetryTopics()
                .create(kafkaTemplate);
    }
}
