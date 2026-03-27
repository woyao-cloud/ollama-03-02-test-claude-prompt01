package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.AuditLog;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Log Event for Kafka messaging
 * Serializable event representing an audit log entry
 *
 * @author Service Team
 * @since 1.0
 */
public class AuditLogEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String username;
    private AuditLog.OperationType operation;
    private String resourceType;
    private UUID resourceId;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String description;
    private String clientIp;
    private String userAgent;
    private String sessionId;
    private boolean success;
    private String errorMessage;
    private Integer executionTimeMs;
    private Instant timestamp;

    /**
     * Default constructor for deserialization
     */
    public AuditLogEvent() {
    }

    /**
     * Private constructor - use builder
     */
    private AuditLogEvent(Builder builder) {
        this.userId = builder.userId;
        this.username = builder.username;
        this.operation = builder.operation;
        this.resourceType = builder.resourceType;
        this.resourceId = builder.resourceId;
        this.oldValue = builder.oldValue;
        this.newValue = builder.newValue;
        this.description = builder.description;
        this.clientIp = builder.clientIp;
        this.userAgent = builder.userAgent;
        this.sessionId = builder.sessionId;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.executionTimeMs = builder.executionTimeMs;
        this.timestamp = builder.timestamp;
    }

    // Getters and Setters

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuditLog.OperationType getOperation() {
        return operation;
    }

    public void setOperation(AuditLog.OperationType operation) {
        this.operation = operation;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, Object> getOldValue() {
        return oldValue;
    }

    public void setOldValue(Map<String, Object> oldValue) {
        this.oldValue = oldValue;
    }

    public Map<String, Object> getNewValue() {
        return newValue;
    }

    public void setNewValue(Map<String, Object> newValue) {
        this.newValue = newValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Integer executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Builder factory method
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for AuditLogEvent
     */
    public static class Builder {
        private UUID userId;
        private String username;
        private AuditLog.OperationType operation;
        private String resourceType;
        private UUID resourceId;
        private Map<String, Object> oldValue;
        private Map<String, Object> newValue;
        private String description;
        private String clientIp;
        private String userAgent;
        private String sessionId;
        private boolean success;
        private String errorMessage;
        private Integer executionTimeMs;
        private Instant timestamp = Instant.now();

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder operation(AuditLog.OperationType operation) {
            this.operation = operation;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(UUID resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder oldValue(Map<String, Object> oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public Builder newValue(Map<String, Object> newValue) {
            this.newValue = newValue;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder executionTimeMs(Integer executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditLogEvent build() {
            return new AuditLogEvent(this);
        }
    }

    @Override
    public String toString() {
        return "AuditLogEvent{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", operation=" + operation +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId=" + resourceId +
                ", success=" + success +
                ", timestamp=" + timestamp +
                '}';
    }
}
