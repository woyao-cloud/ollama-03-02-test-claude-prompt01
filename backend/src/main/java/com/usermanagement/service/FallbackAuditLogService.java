package com.usermanagement.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Fallback Audit Log Service
 * Direct DB write when Kafka is unavailable
 * Extends existing AuditLogServiceImpl for direct database writes
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Primary
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = false)
public class FallbackAuditLogService extends AuditLogServiceImpl {

    /**
     * Constructor - delegates to parent
     */
    public FallbackAuditLogService(
            com.usermanagement.repository.AuditLogRepository auditLogRepository,
            com.usermanagement.repository.UserRepository userRepository,
            com.usermanagement.security.SecurityUtils securityUtils,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        super(auditLogRepository, userRepository, securityUtils, objectMapper);
    }

    // All methods are inherited from AuditLogServiceImpl
    // This service is only active when app.kafka.enabled=false
    // It provides direct database writes as a fallback when Kafka is not available
}
