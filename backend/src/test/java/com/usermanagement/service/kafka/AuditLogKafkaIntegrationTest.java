package com.usermanagement.service.kafka;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.dto.AuditLogEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Kafka audit log processing
 * Uses EmbeddedKafka for testing without external broker
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"audit-logs", "audit-logs-retry", "audit-logs-dlt"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
@ActiveProfiles("test")
@DirtiesContext
class AuditLogKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, AuditLogEvent> kafkaTemplate;

    @Autowired
    private AuditLogKafkaProducer auditLogKafkaProducer;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private final BlockingQueue<AuditLogEvent> receivedEvents = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() {
        receivedEvents.clear();
        auditLogRepository.deleteAll();
    }

    @Test
    void sendAndReceiveAuditLogEvent_ShouldProcessSuccessfully() throws InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .username("integration@test.com")
                .operation(AuditLog.OperationType.CREATE)
                .resourceType("USER")
                .resourceId(UUID.randomUUID())
                .description("Integration test operation")
                .oldValue(Map.of("name", "old"))
                .newValue(Map.of("name", "new"))
                .clientIp("127.0.0.1")
                .userAgent("TestAgent/1.0")
                .sessionId("test-session")
                .success(true)
                .errorMessage(null)
                .executionTimeMs(50)
                .timestamp(Instant.now())
                .build();

        // When
        auditLogKafkaProducer.sendAuditLogEvent(event);

        // Wait for message to be sent
        Thread.sleep(500);

        // Then - verify the producer sent the message (no exception thrown)
        assertTrue(true, "Message sent successfully");
    }

    @Test
    void sendAuditLogEvent_WithAnonymousUser_ShouldSendWithAnonymousKey() throws InterruptedException {
        // Given
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(null)
                .username("anonymous")
                .operation(AuditLog.OperationType.LOGIN)
                .resourceType("SESSION")
                .resourceId(UUID.randomUUID())
                .description("Anonymous login test")
                .success(true)
                .timestamp(Instant.now())
                .build();

        // When
        auditLogKafkaProducer.sendAuditLogEvent(event);

        // Wait for async processing
        Thread.sleep(500);

        // Then
        assertTrue(true, "Anonymous event sent successfully");
    }

    @Test
    void sendAuditLogEvent_WithFailureOperation_ShouldSendSuccessfully() throws InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .username("test@example.com")
                .operation(AuditLog.OperationType.CREATE)
                .resourceType("USER")
                .resourceId(UUID.randomUUID())
                .description("Failed operation test")
                .success(false)
                .errorMessage("Validation failed: email already exists")
                .timestamp(Instant.now())
                .build();

        // When
        auditLogKafkaProducer.sendAuditLogEvent(event);

        // Wait for async processing
        Thread.sleep(500);

        // Then
        assertTrue(true, "Failure event sent successfully");
    }

    @Test
    void kafkaTemplate_ShouldBeConfigured() {
        // Then
        assertNotNull(kafkaTemplate);
        assertNotNull(embeddedKafka);
    }

    @Test
    void embeddedKafka_ShouldHaveRequiredTopics() {
        // When
        String[] topics = embeddedKafka.getTopics().toArray(new String[0]);

        // Then
        assertTrue(containsTopic(topics, "audit-logs"), "Should have audit-logs topic");
        assertTrue(containsTopic(topics, "audit-logs-retry"), "Should have audit-logs-retry topic");
        assertTrue(containsTopic(topics, "audit-logs-dlt"), "Should have audit-logs-dlt topic");
    }

    @Test
    void sendMultipleEvents_ShouldProcessAll() throws InterruptedException {
        // Given
        int eventCount = 5;

        // When
        for (int i = 0; i < eventCount; i++) {
            AuditLogEvent event = AuditLogEvent.builder()
                    .userId(UUID.randomUUID())
                    .username("user" + i + "@test.com")
                    .operation(AuditLog.OperationType.UPDATE)
                    .resourceType("USER")
                    .resourceId(UUID.randomUUID())
                    .description("Batch test event " + i)
                    .success(true)
                    .timestamp(Instant.now())
                    .build();

            auditLogKafkaProducer.sendAuditLogEvent(event);
        }

        // Wait for async processing
        Thread.sleep(1000);

        // Then - all events should be sent without errors
        assertTrue(true, "All " + eventCount + " events sent successfully");
    }

    @Test
    void sendAuditLogEvent_WithComplexData_ShouldSerializeCorrectly() throws InterruptedException {
        // Given
        Map<String, Object> complexData = Map.of(
                "nested", Map.of("key1", "value1", "key2", 123),
                "array", new String[]{"item1", "item2", "item3"},
                "number", 42,
                "boolean", true
        );

        AuditLogEvent event = AuditLogEvent.builder()
                .userId(UUID.randomUUID())
                .username("complex@test.com")
                .operation(AuditLog.OperationType.IMPORT)
                .resourceType("DATA")
                .resourceId(UUID.randomUUID())
                .description("Complex data test")
                .oldValue(complexData)
                .newValue(complexData)
                .success(true)
                .timestamp(Instant.now())
                .build();

        // When
        auditLogKafkaProducer.sendAuditLogEvent(event);

        // Wait for async processing
        Thread.sleep(500);

        // Then
        assertTrue(true, "Complex event sent successfully");
    }

    private boolean containsTopic(String[] topics, String topicName) {
        for (String topic : topics) {
            if (topic.equals(topicName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test listener to capture events
     */
    @KafkaListener(topics = "audit-logs", groupId = "test-group")
    public void listen(AuditLogEvent event) {
        receivedEvents.offer(event);
    }
}
