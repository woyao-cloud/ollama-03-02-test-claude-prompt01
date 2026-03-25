package com.usermanagement.service.kafka;

import com.usermanagement.service.dto.AuditLogEvent;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer for Audit Log Events
 * Publishes audit log events to Kafka topic
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class AuditLogKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaProducer.class);

    private final KafkaTemplate<String, AuditLogEvent> kafkaTemplate;
    private final String auditLogsTopic;

    public AuditLogKafkaProducer(
            KafkaTemplate<String, AuditLogEvent> kafkaTemplate,
            @Value("${app.kafka.topics.audit-logs}") String auditLogsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditLogsTopic = auditLogsTopic;
    }

    /**
     * Send audit log event to Kafka
     *
     * @param event the audit log event to send
     */
    public void sendAuditLogEvent(AuditLogEvent event) {
        if (event == null) {
            logger.warn("Attempted to send null audit log event");
            return;
        }

        String key = event.getUserId() != null ? event.getUserId().toString() : "anonymous";

        try {
            CompletableFuture<SendResult<String, AuditLogEvent>> future =
                    kafkaTemplate.send(auditLogsTopic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send audit log event to Kafka topic {}: {}",
                            auditLogsTopic, ex.getMessage(), ex);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Audit log event sent to topic {} partition {} offset {}: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.getOperation());
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Exception while sending audit log event to Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Send audit log event with custom headers
     *
     * @param event the audit log event
     * @param source the source service/component
     */
    public void sendAuditLogEventWithSource(AuditLogEvent event, String source) {
        if (event == null) {
            logger.warn("Attempted to send null audit log event");
            return;
        }

        String key = event.getUserId() != null ? event.getUserId().toString() : "anonymous";

        ProducerRecord<String, AuditLogEvent> record = new ProducerRecord<>(
                auditLogsTopic, key, event);
        record.headers().add("source", source != null ? source.getBytes() : "unknown".getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());

        try {
            CompletableFuture<SendResult<String, AuditLogEvent>> future =
                    kafkaTemplate.send(record);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send audit log event with source to Kafka: {}", ex.getMessage(), ex);
                } else {
                    logger.debug("Audit log event with source sent successfully");
                }
            });

        } catch (Exception e) {
            logger.error("Exception while sending audit log event with source: {}", e.getMessage(), e);
        }
    }
}
