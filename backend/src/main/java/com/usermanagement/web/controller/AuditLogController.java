package com.usermanagement.web.controller;

import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.service.AuditLogService;
import com.usermanagement.service.dto.AuditLogDTO;
import com.usermanagement.service.dto.AuditLogQueryRequest;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.PageResponse;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Log Controller
 * REST API endpoints for audit log management and querying
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Get all audit logs with pagination and filters
     */
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditLog.OperationType operation,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String clientIp,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.debug("Getting audit logs with filters");

        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setUserId(userId);
        query.setOperation(operation);
        query.setResourceType(resourceType);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setSuccess(success);
        query.setClientIp(clientIp);
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);

        Page<AuditLogDTO> auditLogPage = auditLogService.getAuditLogs(query);
        PageResponse<AuditLogDTO> pageResponse = PageResponse.from(auditLogPage);

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", pageResponse));
    }

    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<AuditLogDTO>> getAuditLogById(@PathVariable UUID id) {
        logger.debug("Getting audit log by ID: {}", id);

        AuditLogDTO auditLog = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log retrieved successfully", auditLog));
    }

    /**
     * Get audit logs for a specific user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ADMIN') or #userId == @securityUtils.getCurrentUserId()")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getUserAuditLogs(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.debug("Getting audit logs for user: {}", userId);

        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);

        Page<AuditLogDTO> auditLogPage = auditLogService.getUserAuditLogs(userId, query);
        PageResponse<AuditLogDTO> pageResponse = PageResponse.from(auditLogPage);

        return ResponseEntity.ok(ApiResponse.success("User audit logs retrieved successfully", pageResponse));
    }

    /**
     * Get audit logs for a specific resource
     */
    @GetMapping("/resource/{resourceType}/{resourceId}")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getResourceAuditLogs(
            @PathVariable String resourceType,
            @PathVariable UUID resourceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.debug("Getting audit logs for resource: {}:{}", resourceType, resourceId);

        AuditLogQueryRequest query = new AuditLogQueryRequest();
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);

        Page<AuditLogDTO> auditLogPage = auditLogService.getResourceAuditLogs(resourceType, resourceId, query);
        PageResponse<AuditLogDTO> pageResponse = PageResponse.from(auditLogPage);

        return ResponseEntity.ok(ApiResponse.success("Resource audit logs retrieved successfully", pageResponse));
    }

    /**
     * Get recent audit logs
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getRecentAuditLogs(
            @RequestParam(defaultValue = "20") int limit) {
        logger.debug("Getting recent audit logs, limit: {}", limit);

        // Enforce reasonable limit
        if (limit > 100) {
            limit = 100;
        }

        List<AuditLogDTO> auditLogs = auditLogService.getRecentAuditLogs(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent audit logs retrieved successfully", auditLogs));
    }

    /**
     * Get audit log statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        logger.debug("Getting audit log statistics");

        // Default to last 30 days if no time range specified
        if (startTime == null) {
            startTime = Instant.now().minusSeconds(30 * 24 * 60 * 60L);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        Map<String, Object> statistics = auditLogService.getStatistics(startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", statistics));
    }

    /**
     * Export audit logs
     */
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('AUDIT_EXPORT') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<String>> exportAuditLogs(
            @Valid @RequestBody AuditLogQueryRequest query,
            @RequestParam(defaultValue = "csv") String format) {
        logger.info("Exporting audit logs in {} format", format);

        String filePath = auditLogService.exportAuditLogs(query, format);
        return ResponseEntity.ok(ApiResponse.success("Audit logs exported successfully", filePath));
    }

    /**
     * Clean up old audit logs
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> cleanupOldLogs(
            @RequestParam(defaultValue = "365") int retentionDays) {
        logger.info("Cleaning up audit logs older than {} days", retentionDays);

        // Enforce minimum retention of 30 days for safety
        if (retentionDays < 30) {
            retentionDays = 30;
        }

        long deletedCount = auditLogService.cleanupOldLogs(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Cleaned up %d audit logs older than %d days", deletedCount, retentionDays),
                deletedCount));
    }
}
