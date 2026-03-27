package com.usermanagement.service.kafka;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.dto.AuditLogEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogKafkaConsumer
 */
@ExtendWith(MockitoExtension.class)
class AuditLogKafkaConsumerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    private AuditLogKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditLogKafkaConsumer(auditLogRepository, userRepository);
    }

    @Test
    void consumeAuditLogEvent_WithValidEvent_ShouldSaveToDatabase() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");

        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .username("test@example.com")
                .operation(AuditLog.OperationType.CREATE)
                .resourceType("USER")
                .resourceId(resourceId)
                .description("Created user")
                .oldValue(Map.of("name", "old"))
                .newValue(Map.of("name", "new"))
                .clientIp("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .sessionId("session-123")
                .success(true)
                .errorMessage(null)
                .executionTimeMs(100)
                .timestamp(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        // When
        consumer.consumeAuditLogEvent(event, 0, 123L, userId.toString());

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertEquals(user, savedLog.getUser());
        assertEquals("test@example.com", savedLog.getUsername());
        assertEquals(AuditLog.OperationType.CREATE, savedLog.getOperation());
        assertEquals("USER", savedLog.getResourceType());
        assertEquals(resourceId, savedLog.getResourceId());
        assertEquals("Created user", savedLog.getDescription());
        assertEquals("192.168.1.1", savedLog.getClientIp());
        assertEquals("Mozilla/5.0", savedLog.getUserAgent());
        assertEquals("session-123", savedLog.getSessionId());
        assertTrue(savedLog.getSuccess());
        assertEquals(100, savedLog.getExecutionTimeMs());
    }

    @Test
    void consumeAuditLogEvent_WithNullEvent_ShouldNotThrow() {
        // When & Then - should not throw
        assertDoesNotThrow(() -> consumer.consumeAuditLogEvent(null, 0, 123L, "key"));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void consumeAuditLogEvent_WithNonExistentUser_ShouldSetNullUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .username("unknown@example.com")
                .operation(AuditLog.OperationType.DELETE)
                .resourceType("USER")
                .resourceId(resourceId)
                .success(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        // When
        consumer.consumeAuditLogEvent(event, 0, 123L, userId.toString());

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertNull(savedLog.getUser());
        assertEquals("unknown@example.com", savedLog.getUsername());
    }

    @Test
    void consumeAuditLogEvent_WithRepositoryError_ShouldThrowException() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .operation(AuditLog.OperationType.UPDATE)
                .resourceType("USER")
                .success(true)
                .build();

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(AuditLogKafkaConsumer.AuditLogProcessingException.class, () ->
                consumer.consumeAuditLogEvent(event, 0, 123L, userId.toString()));
    }

    @Test
    void consumeRetryEvent_WithValidEvent_ShouldSaveToDatabase() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .operation(AuditLog.OperationType.LOGIN)
                .resourceType("SESSION")
                .success(true)
                .build();

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        // When
        consumer.consumeRetryEvent(event, 1, 456L);

        // Then
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void consumeRetryEvent_WithNullEvent_ShouldNotThrow() {
        // When & Then - should not throw
        assertDoesNotThrow(() -> consumer.consumeRetryEvent(null, 0, 123L));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void consumeRetryEvent_WithError_ShouldThrowException() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .operation(AuditLog.OperationType.LOGOUT)
                .resourceType("SESSION")
                .success(true)
                .build();

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Retry failed"));

        // When & Then
        assertThrows(AuditLogKafkaConsumer.AuditLogProcessingException.class, () ->
                consumer.consumeRetryEvent(event, 0, 123L));
    }

    @Test
    void consumeDeadLetterEvent_WithValidEvent_ShouldLogError() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(userId)
                .username("test@example.com")
                .operation(AuditLog.OperationType.EXPORT)
                .resourceType("DATA")
                .success(false)
                .timestamp(Instant.now())
                .build();

        String errorMessage = "Processing failed after retries";

        // When & Then - should not throw
        assertDoesNotThrow(() ->
                consumer.consumeDeadLetterEvent(event, 2, 789L, errorMessage, "stack trace"));
    }

    @Test
    void consumeDeadLetterEvent_WithNullEvent_ShouldNotThrow() {
        // When & Then - should not throw
        assertDoesNotThrow(() ->
                consumer.consumeDeadLetterEvent(null, 0, 123L, "error", null));
    }

    @Test
    void consumeAuditLogEvent_WithNullUserId_ShouldNotQueryUser() {
        // Given
        UUID resourceId = UUID.randomUUID();

        AuditLogEvent event = AuditLogEvent.builder()
                .userId(null)
                .username("anonymous")
                .operation(AuditLog.OperationType.VIEW)
                .resourceType("REPORT")
                .resourceId(resourceId)
                .success(true)
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        // When
        consumer.consumeAuditLogEvent(event, 0, 123L, "anonymous");

        // Then
        verify(userRepository, never()).findById(any());
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertNull(savedLog.getUser());
        assertEquals("anonymous", savedLog.getUsername());
    }
}
