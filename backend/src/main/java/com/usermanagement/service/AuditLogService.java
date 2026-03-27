package com.usermanagement.service;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.service.dto.AuditLogDTO;
import com.usermanagement.service.dto.AuditLogQueryRequest;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Log Service Interface
 * Handles audit log recording and querying
 *
 * @author Service Team
 * @since 1.0
 */
public interface AuditLogService {

    /**
     * Log an operation
     *
     * @param operation operation type
     * @param resourceType resource type
     * @param resourceId resource ID
     * @param description description
     * @param oldValue old value (for updates)
     * @param newValue new value
     * @param success whether operation succeeded
     * @param errorMessage error message if failed
     */
    void logOperation(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            boolean success,
            String errorMessage
    );

    /**
     * Log a successful operation
     *
     * @param operation operation type
     * @param resourceType resource type
     * @param resourceId resource ID
     * @param description description
     * @param newValue new value
     */
    void logSuccess(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            Map<String, Object> newValue
    );

    /**
     * Log a failed operation
     *
     * @param operation operation type
     * @param resourceType resource type
     * @param resourceId resource ID
     * @param description description
     * @param errorMessage error message
     */
    void logFailure(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            String errorMessage
    );

    /**
     * Get audit logs with pagination and filters
     *
     * @param query query request
     * @return page of audit log DTOs
     */
    Page<AuditLogDTO> getAuditLogs(AuditLogQueryRequest query);

    /**
     * Get audit log by ID
     *
     * @param id log ID
     * @return audit log DTO
     */
    AuditLogDTO getAuditLogById(UUID id);

    /**
     * Get audit logs for a specific user
     *
     * @param userId user ID
     * @param query query request
     * @return page of audit log DTOs
     */
    Page<AuditLogDTO> getUserAuditLogs(UUID userId, AuditLogQueryRequest query);

    /**
     * Get audit logs for a specific resource
     *
     * @param resourceType resource type
     * @param resourceId resource ID
     * @param query query request
     * @return page of audit log DTOs
     */
    Page<AuditLogDTO> getResourceAuditLogs(String resourceType, UUID resourceId, AuditLogQueryRequest query);

    /**
     * Get recent audit logs
     *
     * @param limit number of logs to retrieve
     * @return list of audit log DTOs
     */
    List<AuditLogDTO> getRecentAuditLogs(int limit);

    /**
     * Get audit log statistics
     *
     * @param startTime start time
     * @param endTime end time
     * @return statistics map
     */
    Map<String, Object> getStatistics(Instant startTime, Instant endTime);

    /**
     * Export audit logs to file
     *
     * @param query query request
     * @param format export format (csv, json)
     * @return file path
     */
    String exportAuditLogs(AuditLogQueryRequest query, String format);

    /**
     * Clean up old audit logs
     *
     * @param retentionDays number of days to retain
     * @return number of logs deleted
     */
    long cleanupOldLogs(int retentionDays);
}
