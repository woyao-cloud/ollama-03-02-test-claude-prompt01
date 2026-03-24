package com.usermanagement.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * System Configuration Entity
 * Stores system-wide configuration settings
 *
 * @author Domain Team
 * @since 1.0
 */
@Entity
@Table(name = "system_config", indexes = {
    @Index(name = "idx_system_config_key", columnList = "config_key", unique = true),
    @Index(name = "idx_system_config_category", columnList = "category")
})
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConfigCategory category = ConfigCategory.GENERAL;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Configuration category enum
    public enum ConfigCategory {
        GENERAL("通用配置"),
        SECURITY("安全配置"),
        PASSWORD("密码策略"),
        SESSION("会话配置"),
        EMAIL("邮件配置"),
        AUDIT("审计配置");

        private final String description;

        ConfigCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors
    public SystemConfig() {}

    public SystemConfig(String key, String value, ConfigCategory category) {
        this.key = key;
        this.value = value;
        this.category = category;
    }

    public SystemConfig(String key, String value, String description, ConfigCategory category) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.category = category;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConfigCategory getCategory() {
        return category;
    }

    public void setCategory(ConfigCategory category) {
        this.category = category;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public boolean isSystemConfig() {
        return isSystem != null && isSystem;
    }

    /**
     * Get value as integer
     */
    public Integer getValueAsInt() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get value as boolean
     */
    public Boolean getValueAsBoolean() {
        return Boolean.parseBoolean(value);
    }

    /**
     * Get value as long
     */
    public Long getValueAsLong() {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "SystemConfig{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", category=" + category +
                '}';
    }
}
