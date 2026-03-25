package com.usermanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.security.SecurityConfig;
import com.usermanagement.service.AuditLogService;
import com.usermanagement.service.dto.AuditLogDTO;
import com.usermanagement.service.dto.AuditLogQueryRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Audit Log Controller Test
 *
 * @author Test Team
 * @since 1.0
 */
@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditLogService auditLogService;

    private UUID testLogId;
    private AuditLogDTO testAuditLogDTO;

    @BeforeEach
    void setUp() {
        testLogId = UUID.randomUUID();
        testAuditLogDTO = createTestAuditLogDTO(testLogId);
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldGetAuditLogsWithFilters() throws Exception {
        // Given
        List<AuditLogDTO> logs = Arrays.asList(
                testAuditLogDTO,
                createTestAuditLogDTO(UUID.randomUUID())
        );
        Page<AuditLogDTO> logPage = new PageImpl<>(logs);

        when(auditLogService.getAuditLogs(Mockito.any(AuditLogQueryRequest.class))).thenReturn(logPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("page", "0")
                        .param("size", "20")
                        .param("operation", "CREATE")
                        .param("resourceType", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldGetAuditLogById() throws Exception {
        // Given
        when(auditLogService.getAuditLogById(testLogId)).thenReturn(testAuditLogDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs/{id}", testLogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testLogId.toString()));
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldGetUserAuditLogs() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Page<AuditLogDTO> logPage = new PageImpl<>(Collections.singletonList(testAuditLogDTO));

        when(auditLogService.getUserAuditLogs(eq(userId), Mockito.any(AuditLogQueryRequest.class))).thenReturn(logPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldGetResourceAuditLogs() throws Exception {
        // Given
        UUID resourceId = UUID.randomUUID();
        String resourceType = "USER";
        Page<AuditLogDTO> logPage = new PageImpl<>(Collections.singletonList(testAuditLogDTO));

        when(auditLogService.getResourceAuditLogs(eq(resourceType), eq(resourceId), Mockito.any(AuditLogQueryRequest.class)))
                .thenReturn(logPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs/resource/{resourceType}/{resourceId}", resourceType, resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldGetRecentAuditLogs() throws Exception {
        // Given
        List<AuditLogDTO> logs = Arrays.asList(
                testAuditLogDTO,
                createTestAuditLogDTO(UUID.randomUUID())
        );

        when(auditLogService.getRecentAuditLogs(20)).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldGetStatistics() throws Exception {
        // Given
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", 100L);
        stats.put("successCount", 95L);
        stats.put("failureCount", 5L);
        stats.put("successRate", 95.0);

        when(auditLogService.getStatistics(Mockito.any(Instant.class), Mockito.any(Instant.class))).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(100))
                .andExpect(jsonPath("$.data.successRate").value(95.0));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldExportAuditLogs() throws Exception {
        // Given
        AuditLogQueryRequest request = new AuditLogQueryRequest();
        request.setOperation(AuditLog.OperationType.CREATE);

        String exportPath = "/tmp/audit-logs-2024-01-01.csv";
        when(auditLogService.exportAuditLogs(Mockito.any(AuditLogQueryRequest.class), eq("csv")))
                .thenReturn(exportPath);

        // When & Then
        mockMvc.perform(post("/api/v1/audit-logs/export")
                        .with(csrf())
                        .param("format", "csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(exportPath));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldCleanupOldLogs() throws Exception {
        // Given
        when(auditLogService.cleanupOldLogs(365)).thenReturn(1000L);

        // When & Then
        mockMvc.perform(post("/api/v1/audit-logs/cleanup")
                        .with(csrf())
                        .param("retentionDays", "365"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1000));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldEnforceMinimumRetentionDays() throws Exception {
        // Given
        when(auditLogService.cleanupOldLogs(30)).thenReturn(0L);

        // When & Then - when requesting less than 30 days, should use 30
        mockMvc.perform(post("/api/v1/audit-logs/cleanup")
                        .with(csrf())
                        .param("retentionDays", "7"))
                .andExpect(status().isOk());

        verify(auditLogService).cleanupOldLogs(30);
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldDenyAccessWithoutAuditAuthority() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    void shouldHandleServiceException() throws Exception {
        // Given
        when(auditLogService.getAuditLogById(Mockito.any())).thenThrow(new IllegalArgumentException("Audit log not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/audit-logs/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    // Helper method
    private AuditLogDTO createTestAuditLogDTO(UUID id) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(id);
        dto.setUserId(UUID.randomUUID());
        dto.setUsername("test@example.com");
        dto.setOperation(AuditLog.OperationType.CREATE);
        dto.setOperationDescription("创建");
        dto.setResourceType("USER");
        dto.setResourceId(UUID.randomUUID());
        dto.setDescription("Created user");
        dto.setSuccess(true);
        dto.setClientIp("192.168.1.1");
        dto.setCreatedAt(Instant.now());
        return dto;
    }
}
