package com.usermanagement.service.kafka;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.dto.AuditLogEvent;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka Consumer for Audit Log Events
 * Consumes from Kafka topic and persists to database
 *
 * @author Service Team
 * @since 1.0
 */
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class AuditLogKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogKafkaConsumer(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consume audit log event from Kafka main topic
     */
    @KafkaListener(
            topics = "${app.kafka.topics.audit-logs}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeAuditLogEvent(
            @Payload AuditLogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        if (event == null) {
            logger.warn("Received null audit log event from partition {} offset {}", partition, offset);
            return;
        }

        logger.debug("Received audit log event from partition {} offset {} key {}: operation={}",
                partition, offset, key, event.getOperation());

        try {
            AuditLog auditLog = convertToEntity(event);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log saved to database: id={}, operation={}",
                    auditLog.getId(), auditLog.getOperation());

        } catch (Exception e) {
            logger.error("Failed to process audit log event from partition {} offset {}: {}",
                    partition, offset, e.getMessage(), e);
            // Re-throw to trigger retry/DLT handling
            throw new AuditLogProcessingException("Failed to process audit log event", e);
        }
    }

    /**
     * Retry listener for failed messages
     */
    @KafkaListener(
            topics = "${app.kafka.topics.audit-logs-retry}",
            groupId = "${spring.kafka.consumer.group-id}-retry",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeRetryEvent(
            @Payload AuditLogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.warn("Processing retry for audit log event: operation={}, partition={}, offset={}",
                event != null ? event.getOperation() : "null", partition, offset);

        if (event == null) {
            logger.warn("Received null event in retry topic");
            return;
        }

        try {
            AuditLog auditLog = convertToEntity(event);
            auditLogRepository.save(auditLog);
            logger.info("Audit log saved successfully on retry: id={}, operation={}",
                    auditLog.getId(), auditLog.getOperation());

        } catch (Exception e) {
            logger.error("Failed to process audit log event on retry: {}", e.getMessage(), e);
            throw new AuditLogProcessingException("Failed to process audit log on retry", e);
        }
    }

    /**
     * Dead letter topic listener for permanently failed messages
     */
    @KafkaListener(
            topics = "${app.kafka.topics.audit-logs-dlt}",
            groupId = "${spring.kafka.consumer.group-id}-dlt",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDeadLetterEvent(
            @Payload AuditLogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage,
            @Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace) {

        logger.error("Audit log event moved to DLT: " +
                        "operation={}, partition={}, offset={}, error={}",
                event != null ? event.getOperation() : "null",
                partition, offset, errorMessage);

        // Log full event details for manual inspection/reprocessing
        if (event != null) {
            logger.error("DLT Event Details: userId={}, username={}, resourceType={}, " +
                            "resourceId={}, success={}, timestamp={}",
                    event.getUserId(), event.getUsername(), event.getResourceType(),
                    event.getResourceId(), event.isSuccess(), event.getTimestamp());
        }

        // TODO: Alert/notify ops team about failed audit log
        // This could integrate with monitoring systems like PagerDuty, Slack, etc.
    }

    /**
     * Convert AuditLogEvent DTO to AuditLog entity
     */
    private AuditLog convertToEntity(AuditLogEvent event) {
        AuditLog auditLog = new AuditLog();

        // Set user reference if userId is present
        if (event.getUserId() != null) {
            try {
                User user = userRepository.findById(event.getUserId()).orElse(null);
                auditLog.setUser(user);
            } catch (Exception e) {
                logger.warn("Could not find user {} for audit log: {}", event.getUserId(), e.getMessage());
            }
        }

        auditLog.setUsername(event.getUsername() != null ? event.getUsername() : "anonymous");
        auditLog.setOperation(event.getOperation());
        auditLog.setResourceType(event.getResourceType());
        auditLog.setResourceId(event.getResourceId());
        auditLog.setOldValue(event.getOldValue());
        auditLog.setNewValue(event.getNewValue());
        auditLog.setDescription(event.getDescription());
        auditLog.setClientIp(event.getClientIp());
        auditLog.setUserAgent(event.getUserAgent());
        auditLog.setSessionId(event.getSessionId());
        auditLog.setSuccess(event.isSuccess());
        auditLog.setErrorMessage(event.getErrorMessage());
        auditLog.setExecutionTimeMs(event.getExecutionTimeMs());

        // Use event timestamp if available, otherwise createdAt will be set by @CreationTimestamp
        if (event.getTimestamp() != null) {
            auditLog.setCreatedAt(event.getTimestamp());
        }

        return auditLog;
    }

    /**
     * Custom exception for audit log processing errors
     */
    public static class AuditLogProcessingException extends RuntimeException {
        public AuditLogProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
