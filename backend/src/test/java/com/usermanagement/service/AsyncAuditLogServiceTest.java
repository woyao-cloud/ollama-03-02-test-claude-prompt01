package com.usermanagement.service;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.service.dto.AuditLogDTO;
import com.usermanagement.service.dto.AuditLogEvent;
import com.usermanagement.service.dto.AuditLogQueryRequest;
import com.usermanagement.service.kafka.AuditLogKafkaProducer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AsyncAuditLogService
 */
@ExtendWith(MockitoExtension.class)
class AsyncAuditLogServiceTest {

    @Mock
    private AuditLogKafkaProducer kafkaProducer;

    @Mock
    private AuditLogRepository auditLogRepository;

    private AsyncAuditLogService asyncAuditLogService;

    @BeforeEach
    void setUp() {
        asyncAuditLogService = new AsyncAuditLogService(kafkaProducer, auditLogRepository);
    }

    @Test
    void logOperation_WithValidData_ShouldSendToKafka() {
        // Given
        UUID resourceId = UUID.randomUUID();
        Map<String, Object> oldValue = Map.of("name", "Old Name");
        Map<String, Object> newValue = Map.of("name", "New Name");

        // When
        asyncAuditLogService.logOperation(
                AuditLog.OperationType.UPDATE,
                "USER",
                resourceId,
                "Updated user name",
                oldValue,
                newValue,
                true,
                null
        );

        // Then
        ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(kafkaProducer).sendAuditLogEvent(eventCaptor.capture());

        AuditLogEvent capturedEvent = eventCaptor.getValue();
        assertEquals(AuditLog.OperationType.UPDATE, capturedEvent.getOperation());
        assertEquals("USER", capturedEvent.getResourceType());
        assertEquals(resourceId, capturedEvent.getResourceId());
        assertEquals("Updated user name", capturedEvent.getDescription());
        assertEquals(oldValue, capturedEvent.getOldValue());
        assertEquals(newValue, capturedEvent.getNewValue());
        assertTrue(capturedEvent.isSuccess());
        assertNull(capturedEvent.getErrorMessage());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void logOperation_WithFailure_ShouldNotThrow() {
        // Given
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(kafkaProducer).sendAuditLogEvent(any(AuditLogEvent.class));

        // When & Then - should not throw
        assertDoesNotThrow(() ->
                asyncAuditLogService.logOperation(
                        AuditLog.OperationType.CREATE,
                        "USER",
                        UUID.randomUUID(),
                        "Test",
                        null,
                        null,
                        true,
                        null
                )
        );
    }

    @Test
    void logSuccess_ShouldSendToKafka() {
        // Given
        UUID resourceId = UUID.randomUUID();
        Map<String, Object> newValue = Map.of("status", "active");

        // When
        asyncAuditLogService.logSuccess(
                AuditLog.OperationType.CREATE,
                "USER",
                resourceId,
                "Created new user",
                newValue
        );

        // Then
        ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(kafkaProducer).sendAuditLogEvent(eventCaptor.capture());

        AuditLogEvent capturedEvent = eventCaptor.getValue();
        assertEquals(AuditLog.OperationType.CREATE, capturedEvent.getOperation());
        assertEquals("USER", capturedEvent.getResourceType());
        assertEquals(resourceId, capturedEvent.getResourceId());
        assertEquals("Created new user", capturedEvent.getDescription());
        assertNull(capturedEvent.getOldValue());
        assertEquals(newValue, capturedEvent.getNewValue());
        assertTrue(capturedEvent.isSuccess());
        assertNull(capturedEvent.getErrorMessage());
    }

    @Test
    void logFailure_ShouldSendToKafka() {
        // Given
        UUID resourceId = UUID.randomUUID();
        String errorMessage = "Invalid email format";

        // When
        asyncAuditLogService.logFailure(
                AuditLog.OperationType.CREATE,
                "USER",
                resourceId,
                "Failed to create user",
                errorMessage
        );

        // Then
        ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(kafkaProducer).sendAuditLogEvent(eventCaptor.capture());

        AuditLogEvent capturedEvent = eventCaptor.getValue();
        assertEquals(AuditLog.OperationType.CREATE, capturedEvent.getOperation());
        assertEquals("USER", capturedEvent.getResourceType());
        assertEquals(resourceId, capturedEvent.getResourceId());
        assertEquals("Failed to create user", capturedEvent.getDescription());
        assertNull(capturedEvent.getOldValue());
        assertNull(capturedEvent.getNewValue());
        assertFalse(capturedEvent.isSuccess());
        assertEquals(errorMessage, capturedEvent.getErrorMessage());
    }

    @Test
    void getAuditLogs_ShouldDelegateToRepository() {
        // Given
        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(0);
        query.setSize(10);

        AuditLog auditLog = createSampleAuditLog();
        Page<AuditLog> auditLogPage = new PageImpl<>(
                Collections.singletonList(auditLog),
                PageRequest.of(0, 10),
                1
        );

        when(auditLogRepository.findByConditions(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(auditLogPage);

        // When
        Page<AuditLogDTO> result = asyncAuditLogService.getAuditLogs(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByConditions(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getAuditLogById_WithExistingId_ShouldReturnDTO() {
        // Given
        UUID id = UUID.randomUUID();
        AuditLog auditLog = createSampleAuditLog();
        auditLog.setId(id);

        when(auditLogRepository.findById(id)).thenReturn(Optional.of(auditLog));

        // When
        AuditLogDTO result = asyncAuditLogService.getAuditLogById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(auditLog.getUsername(), result.getUsername());
        assertEquals(auditLog.getOperation(), result.getOperation());
    }

    @Test
    void getAuditLogById_WithNonExistingId_ShouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        when(auditLogRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> asyncAuditLogService.getAuditLogById(id));
    }

    @Test
    void getUserAuditLogs_ShouldDelegateToRepository() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(0);
        query.setSize(10);

        Page<AuditLog> auditLogPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(auditLogRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(auditLogPage);

        // When
        Page<AuditLogDTO> result = asyncAuditLogService.getUserAuditLogs(userId, query);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(auditLogRepository).findByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getResourceAuditLogs_ShouldDelegateToRepository() {
        // Given
        String resourceType = "USER";
        UUID resourceId = UUID.randomUUID();
        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(0);
        query.setSize(10);

        Page<AuditLog> auditLogPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(auditLogRepository.findByResourceTypeAndResourceId(
                eq(resourceType), eq(resourceId), any(Pageable.class)))
                .thenReturn(auditLogPage);

        // When
        Page<AuditLogDTO> result = asyncAuditLogService.getResourceAuditLogs(
                resourceType, resourceId, query);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(auditLogRepository).findByResourceTypeAndResourceId(
                eq(resourceType), eq(resourceId), any(Pageable.class));
    }

    @Test
    void getRecentAuditLogs_ShouldDelegateToRepository() {
        // Given
        int limit = 5;
        AuditLog auditLog = createSampleAuditLog();

        when(auditLogRepository.findRecent(any(Pageable.class)))
                .thenReturn(Collections.singletonList(auditLog));

        // When
        List<AuditLogDTO> result = asyncAuditLogService.getRecentAuditLogs(limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(auditLogRepository).findRecent(any(Pageable.class));
    }

    @Test
    void getStatistics_ShouldReturnStatistics() {
        // Given
        Instant startTime = Instant.now().minusSeconds(86400);
        Instant endTime = Instant.now();

        when(auditLogRepository.countByTimeRange(startTime, endTime)).thenReturn(100L);
        when(auditLogRepository.countByOperation(any(AuditLog.OperationType.class))).thenReturn(10L);
        when(auditLogRepository.findBySuccess(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 95));
        when(auditLogRepository.findBySuccess(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 5));

        // When
        Map<String, Object> result = asyncAuditLogService.getStatistics(startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.get("totalCount"));
        assertEquals(95L, result.get("successCount"));
        assertEquals(5L, result.get("failureCount"));
        assertEquals(95.0, result.get("successRate"));
        assertNotNull(result.get("operationCounts"));
    }

    @Test
    void cleanupOldLogs_ShouldDeleteOldLogs() {
        // Given
        int retentionDays = 30;
        AuditLog oldLog = createSampleAuditLog();

        Page<AuditLog> oldLogsPage = new PageImpl<>(
                Collections.singletonList(oldLog), PageRequest.of(0, 10), 1);

        when(auditLogRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(oldLogsPage);

        // When
        long result = asyncAuditLogService.cleanupOldLogs(retentionDays);

        // Then
        assertEquals(1L, result);
        verify(auditLogRepository).delete(oldLog);
    }

    private AuditLog createSampleAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUsername("test@example.com");
        auditLog.setOperation(AuditLog.OperationType.CREATE);
        auditLog.setResourceType("USER");
        auditLog.setResourceId(UUID.randomUUID());
        auditLog.setDescription("Test operation");
        auditLog.setSuccess(true);
        auditLog.setCreatedAt(Instant.now());
        return auditLog;
    }
}
