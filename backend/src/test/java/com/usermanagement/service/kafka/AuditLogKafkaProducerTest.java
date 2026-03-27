package com.usermanagement.service.kafka;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.service.dto.AuditLogEvent;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogKafkaProducer
 */
@ExtendWith(MockitoExtension.class)
class AuditLogKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, AuditLogEvent> kafkaTemplate;

    private AuditLogKafkaProducer producer;

    private static final String TOPIC = "audit-logs";

    @BeforeEach
    void setUp() {
        producer = new AuditLogKafkaProducer(kafkaTemplate, TOPIC);
    }

    @Test
    void sendAuditLogEvent_WithValidEvent_ShouldSendToKafka() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .username("test@example.com")
                .operation(AuditLog.OperationType.CREATE)
                .resourceType("USER")
                .resourceId(UUID.randomUUID())
                .description("Test operation")
                .success(true)
                .build();

        SendResult<String, AuditLogEvent> sendResult = createSendResult(TOPIC, 0, 123L);
        CompletableFuture<SendResult<String, AuditLogEvent>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(TOPIC), eq(userId.toString()), any(AuditLogEvent.class)))
                .thenReturn(future);

        // When
        producer.sendAuditLogEvent(event);

        // Then
        verify(kafkaTemplate).send(eq(TOPIC), eq(userId.toString()), eq(event));
    }

    @Test
    void sendAuditLogEvent_WithAnonymousUser_ShouldUseAnonymousKey() {
        // Given
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(null)
                .username("anonymous")
                .operation(AuditLog.OperationType.LOGIN)
                .resourceType("SESSION")
                .success(true)
                .build();

        SendResult<String, AuditLogEvent> sendResult = createSendResult(TOPIC, 1, 456L);
        CompletableFuture<SendResult<String, AuditLogEvent>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(TOPIC), eq("anonymous"), any(AuditLogEvent.class)))
                .thenReturn(future);

        // When
        producer.sendAuditLogEvent(event);

        // Then
        verify(kafkaTemplate).send(eq(TOPIC), eq("anonymous"), eq(event));
    }

    @Test
    void sendAuditLogEvent_WithNullEvent_ShouldNotSend() {
        // When
        producer.sendAuditLogEvent(null);

        // Then
        verify(kafkaTemplate, never()).send(any(), any(), any(AuditLogEvent.class));
    }

    @Test
    void sendAuditLogEvent_WithSendFailure_ShouldNotThrow() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .operation(AuditLog.OperationType.UPDATE)
                .resourceType("USER")
                .success(true)
                .build();

        CompletableFuture<SendResult<String, AuditLogEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection failed"));

        when(kafkaTemplate.send(eq(TOPIC), eq(userId.toString()), any(AuditLogEvent.class)))
                .thenReturn(future);

        // When & Then - should not throw
        assertDoesNotThrow(() -> producer.sendAuditLogEvent(event));
    }

    @Test
    void sendAuditLogEvent_WithException_ShouldNotThrow() {
        // Given
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(UUID.randomUUID())
                .operation(AuditLog.OperationType.DELETE)
                .resourceType("USER")
                .success(true)
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any(AuditLogEvent.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then - should not throw
        assertDoesNotThrow(() -> producer.sendAuditLogEvent(event));
    }

    @Test
    void sendAuditLogEventWithSource_ShouldAddHeaders() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .operation(AuditLog.OperationType.CREATE)
                .resourceType("USER")
                .success(true)
                .build();

        String source = "UserService";

        SendResult<String, AuditLogEvent> sendResult = createSendResult(TOPIC, 0, 789L);
        CompletableFuture<SendResult<String, AuditLogEvent>> future = CompletableFuture.completedFuture(sendResult);

        ArgumentCaptor<ProducerRecord<String, AuditLogEvent>> recordCaptor =
                ArgumentCaptor.forClass(ProducerRecord.class);

        when(kafkaTemplate.send(recordCaptor.capture())).thenReturn(future);

        // When
        producer.sendAuditLogEventWithSource(event, source);

        // Then
        ProducerRecord<String, AuditLogEvent> capturedRecord = recordCaptor.getValue();
        assertEquals(TOPIC, capturedRecord.topic());
        assertEquals(userId.toString(), capturedRecord.key());
        assertEquals(event, capturedRecord.value());
        assertNotNull(capturedRecord.headers().lastHeader("source"));
        assertNotNull(capturedRecord.headers().lastHeader("timestamp"));
    }

    @Test
    void sendAuditLogEventWithSource_WithNullEvent_ShouldNotSend() {
        // When
        producer.sendAuditLogEventWithSource(null, "TestService");

        // Then
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    private SendResult<String, AuditLogEvent> createSendResult(String topic, int partition, long offset) {
        ProducerRecord<String, AuditLogEvent> producerRecord =
                new ProducerRecord<>(topic, partition, "key", null);
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(topic, partition), offset, 0, 0, 0, 0);
        return new SendResult<>(producerRecord, recordMetadata);
    }
}
