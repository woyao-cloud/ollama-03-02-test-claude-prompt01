package com.usermanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.SecurityUtilsComponent;
import com.usermanagement.service.dto.AuditLogDTO;
import com.usermanagement.service.dto.AuditLogQueryRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Audit Log Service Implementation Test
 *
 * @author Test Team
 * @since 1.0
 */
class AuditLogServiceImplTest {

    private AuditLogServiceImpl auditLogService;
    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private SecurityUtilsComponent securityUtils;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        //securityUtils = mock(SecurityUtilsComponent.class);
        objectMapper = new ObjectMapper();

        auditLogService = new AuditLogServiceImpl(
                auditLogRepository,
                userRepository,
                //securityUtils,
                objectMapper
        );
    }

    @Test
    void shouldLogSuccessOperation() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        User user = new User();
        user.setId(currentUserId);
        user.setEmail("test@example.com");

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("name", "Test User");

        // When
        auditLogService.logSuccess(
                AuditLog.OperationType.CREATE,
                "USER",
                UUID.randomUUID(),
                "Created user",
                newValue
        );

        // Then
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void shouldLogFailureOperation() {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        // When
        auditLogService.logFailure(
                AuditLog.OperationType.LOGIN,
                "AUTH",
                null,
                "Login failed",
                "Invalid credentials"
        );

        // Then
        verify(auditLogRepository).save(argThat(log ->
                !log.getSuccess() &&
                "Invalid credentials".equals(log.getErrorMessage())
        ));
    }

    @Test
    void shouldGetAuditLogsWithFilters() {
        // Given
        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setOperation(AuditLog.OperationType.CREATE);
        query.setResourceType("USER");
        query.setPage(0);
        query.setSize(10);

        AuditLog auditLog = createTestAuditLog();
        Page<AuditLog> auditLogPage = new PageImpl<>(Collections.singletonList(auditLog));

        when(auditLogRepository.findByConditions(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(auditLogPage);

        // When
        Page<AuditLogDTO> result = auditLogService.getAuditLogs(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldGetAuditLogById() {
        // Given
        UUID logId = UUID.randomUUID();
        AuditLog auditLog = createTestAuditLog();
        auditLog.setId(logId);

        when(auditLogRepository.findById(logId)).thenReturn(Optional.of(auditLog));

        // When
        AuditLogDTO result = auditLogService.getAuditLogById(logId);

        // Then
        assertNotNull(result);
        assertEquals(logId, result.getId());
    }

    @Test
    void shouldThrowExceptionWhenAuditLogNotFound() {
        // Given
        UUID logId = UUID.randomUUID();
        when(auditLogRepository.findById(logId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> auditLogService.getAuditLogById(logId));
        assertTrue(exception.getMessage().contains("Audit log not found"));
    }

    @Test
    void shouldGetUserAuditLogs() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(0);
        query.setSize(10);

        AuditLog auditLog = createTestAuditLog();
        Page<AuditLog> auditLogPage = new PageImpl<>(Collections.singletonList(auditLog));

        when(auditLogRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(auditLogPage);

        // When
        Page<AuditLogDTO> result = auditLogService.getUserAuditLogs(userId, query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldGetResourceAuditLogs() {
        // Given
        UUID resourceId = UUID.randomUUID();
        String resourceType = "USER";
        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(0);
        query.setSize(10);

        AuditLog auditLog = createTestAuditLog();
        Page<AuditLog> auditLogPage = new PageImpl<>(Collections.singletonList(auditLog));

        when(auditLogRepository.findByResourceTypeAndResourceId(
                eq(resourceType), eq(resourceId), any(Pageable.class)
        )).thenReturn(auditLogPage);

        // When
        Page<AuditLogDTO> result = auditLogService.getResourceAuditLogs(resourceType, resourceId, query);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldGetRecentAuditLogs() {
        // Given
        AuditLog auditLog = createTestAuditLog();
        when(auditLogRepository.findRecent(any(Pageable.class)))
                .thenReturn(Collections.singletonList(auditLog));

        // When
        List<AuditLogDTO> result = auditLogService.getRecentAuditLogs(10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetStatistics() {
        // Given
        Instant startTime = Instant.now().minusSeconds(86400);
        Instant endTime = Instant.now();

        when(auditLogRepository.countByTimeRange(startTime, endTime)).thenReturn(100L);
        when(auditLogRepository.findBySuccess(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(auditLogRepository.findBySuccess(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // When
        Map<String, Object> result = auditLogService.getStatistics(startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.get("totalCount"));
    }

    @Test
    void shouldCleanupOldLogs() {
        // Given
        int retentionDays = 30;
        Instant cutoffDate = Instant.now().minusSeconds(retentionDays * 24 * 60 * 60L);

        AuditLog oldLog = createTestAuditLog();
        Page<AuditLog> oldLogsPage = new PageImpl<>(Collections.singletonList(oldLog));

        when(auditLogRepository.findByCreatedAtBetween(
                eq(Instant.EPOCH), any(Instant.class), any(Pageable.class)
        )).thenReturn(oldLogsPage);

        // When
        long result = auditLogService.cleanupOldLogs(retentionDays);

        // Then
        assertEquals(1, result);
        verify(auditLogRepository).delete(any(AuditLog.class));
    }

    private AuditLog createTestAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUsername("test@example.com");
        auditLog.setOperation(AuditLog.OperationType.CREATE);
        auditLog.setResourceType("USER");
        auditLog.setResourceId(UUID.randomUUID());
        auditLog.setDescription("Test audit log");
        auditLog.setSuccess(true);
        auditLog.setCreatedAt(Instant.now());
        return auditLog;
    }
}
