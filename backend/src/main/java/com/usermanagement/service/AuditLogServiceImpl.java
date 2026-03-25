package com.usermanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.AuditLog;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.SecurityUtils;
import com.usermanagement.service.dto.AuditLogDTO;
import com.usermanagement.service.dto.AuditLogQueryRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Audit Log Service Implementation
 * Handles audit log recording and querying
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    //private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            //SecurityUtils securityUtils,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        //this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    @Async("auditLogExecutor")
    @Transactional
    public void logOperation(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            boolean success,
            String errorMessage) {

        try {
            AuditLog auditLog = new AuditLog();

            // Set user info
            UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
            if (currentUserId != null) {
                User user = userRepository.findById(currentUserId).orElse(null);
                auditLog.setUser(user);
                auditLog.setUsername(user != null ? user.getEmail() : "unknown");
            } else {
                auditLog.setUsername("anonymous");
            }

            // Set operation details
            auditLog.setOperation(operation);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMessage);

            // Set client info from request context
            auditLog.setClientIp(getClientIp());
            auditLog.setUserAgent(getUserAgent());
            auditLog.setSessionId(getSessionId());

            auditLogRepository.save(auditLog);
            logger.debug("Audit log saved: {} on {}:{}", operation, resourceType, resourceId);

        } catch (Exception e) {
            logger.error("Failed to save audit log", e);
        }
    }

    @Override
    public void logSuccess(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            Map<String, Object> newValue) {

        logOperation(operation, resourceType, resourceId, description, null, newValue, true, null);
    }

    @Override
    public void logFailure(
            AuditLog.OperationType operation,
            String resourceType,
            UUID resourceId,
            String description,
            String errorMessage) {

        logOperation(operation, resourceType, resourceId, description, null, null, false, errorMessage);
    }

    @Override
    public Page<AuditLogDTO> getAuditLogs(AuditLogQueryRequest query) {
        query.normalize();

        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(query.getSortDirection()) ?
                        Sort.Direction.DESC : Sort.Direction.ASC,
                query.getSortBy()
        );

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        Page<AuditLog> auditLogPage = auditLogRepository.findByConditions(
                query.getUserId(),
                query.getOperation(),
                query.getResourceType(),
                query.getStartTime(),
                query.getEndTime(),
                query.getSuccess(),
                pageable
        );

        return auditLogPage.map(this::convertToDTO);
    }

    @Override
    public AuditLogDTO getAuditLogById(UUID id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found: " + id));
        return convertToDTO(auditLog);
    }

    @Override
    public Page<AuditLogDTO> getUserAuditLogs(UUID userId, AuditLogQueryRequest query) {
        query.normalize();

        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(query.getSortDirection()) ?
                        Sort.Direction.DESC : Sort.Direction.ASC,
                query.getSortBy()
        );

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        Page<AuditLog> auditLogPage = auditLogRepository.findByUserId(userId, pageable);
        return auditLogPage.map(this::convertToDTO);
    }

    @Override
    public Page<AuditLogDTO> getResourceAuditLogs(String resourceType, UUID resourceId, AuditLogQueryRequest query) {
        query.normalize();

        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(query.getSortDirection()) ?
                        Sort.Direction.DESC : Sort.Direction.ASC,
                query.getSortBy()
        );

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        Page<AuditLog> auditLogPage = auditLogRepository.findByResourceTypeAndResourceId(
                resourceType, resourceId, pageable);
        return auditLogPage.map(this::convertToDTO);
    }

    @Override
    public List<AuditLogDTO> getRecentAuditLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findRecent(pageable).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStatistics(Instant startTime, Instant endTime) {
        Map<String, Object> stats = new HashMap<>();

        // Total count in time range
        long totalCount = auditLogRepository.countByTimeRange(startTime, endTime);
        stats.put("totalCount", totalCount);

        // Count by operation type
        Map<String, Long> operationCounts = new HashMap<>();
        for (AuditLog.OperationType op : AuditLog.OperationType.values()) {
            long count = auditLogRepository.countByOperation(op);
            if (count > 0) {
                operationCounts.put(op.name(), count);
            }
        }
        stats.put("operationCounts", operationCounts);

        // Success rate
        long successCount = auditLogRepository.findBySuccess(true, Pageable.unpaged()).getTotalElements();
        long failureCount = auditLogRepository.findBySuccess(false, Pageable.unpaged()).getTotalElements();
        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;

        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        return stats;
    }

    @Override
    @Transactional
    public String exportAuditLogs(AuditLogQueryRequest query, String format) {
        Page<AuditLogDTO> logs = getAuditLogs(query);

        String timestamp = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String filename = "audit-logs-" + timestamp + "." + format.toLowerCase();
        String filepath = System.getProperty("java.io.tmpdir") + File.separator + filename;

        try (FileWriter writer = new FileWriter(filepath)) {
            if ("csv".equalsIgnoreCase(format)) {
                exportToCsv(writer, logs.getContent());
            } else if ("json".equalsIgnoreCase(format)) {
                exportToJson(writer, logs.getContent());
            } else {
                throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Audit logs exported to: {}", filepath);
            return filepath;

        } catch (IOException e) {
            logger.error("Failed to export audit logs", e);
            throw new RuntimeException("Failed to export audit logs", e);
        }
    }

    @Override
    @Transactional
    public long cleanupOldLogs(int retentionDays) {
        Instant cutoffDate = Instant.now().minusSeconds(retentionDays * 24 * 60 * 60L);

        // Get logs to delete
        Page<AuditLog> oldLogs = auditLogRepository.findByCreatedAtBetween(
                Instant.EPOCH, cutoffDate, Pageable.unpaged());

        long count = oldLogs.getTotalElements();

        // Delete in batches
        for (AuditLog log : oldLogs.getContent()) {
            auditLogRepository.delete(log);
        }

        logger.info("Cleaned up {} audit logs older than {} days", count, retentionDays);
        return count;
    }

    private void exportToCsv(FileWriter writer, List<AuditLogDTO> logs) throws IOException {
        // Write header
        writer.write("ID,Username,Operation,Resource Type,Resource ID,Description,Success,Error Message,Client IP,Created At\n");

        // Write data
        for (AuditLogDTO log : logs) {
            writer.write(String.format("%s,%s,%s,%s,%s,\"%s\",%s,\"%s\",%s,%s\n",
                    log.getId(),
                    escapeCsv(log.getUsername()),
                    log.getOperation(),
                    log.getResourceType(),
                    log.getResourceId(),
                    escapeCsv(log.getDescription()),
                    log.isSuccess(),
                    escapeCsv(log.getErrorMessage()),
                    log.getClientIp(),
                    log.getCreatedAt()
            ));
        }
    }

    private void exportToJson(FileWriter writer, List<AuditLogDTO> logs) throws IOException {
        writer.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logs));
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"").replace("\n", " ");
    }

    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setUserId(auditLog.getUser() != null ? auditLog.getUser().getId() : null);
        dto.setUsername(auditLog.getUsername());
        dto.setOperation(auditLog.getOperation());
        dto.setOperationDescription(auditLog.getOperation() != null ?
                auditLog.getOperation().getDescription() : null);
        dto.setResourceType(auditLog.getResourceType());
        dto.setResourceId(auditLog.getResourceId());
        dto.setOldValue(auditLog.getOldValue());
        dto.setNewValue(auditLog.getNewValue());
        dto.setDescription(auditLog.getDescription());
        dto.setClientIp(auditLog.getClientIp());
        dto.setUserAgent(auditLog.getUserAgent());
        dto.setSessionId(auditLog.getSessionId());
        dto.setSuccess(auditLog.getSuccess());
        dto.setErrorMessage(auditLog.getErrorMessage());
        dto.setExecutionTimeMs(auditLog.getExecutionTimeMs());
        dto.setCreatedAt(auditLog.getCreatedAt());
        return dto;
    }

    private String getClientIp() {
        // Get from request context - simplified version
        return null;
    }

    private String getUserAgent() {
        // Get from request context - simplified version
        return null;
    }

    private String getSessionId() {
        // Get from request context - simplified version
        return null;
    }
}
