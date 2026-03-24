package com.usermanagement.service;

import com.usermanagement.domain.entity.SystemConfig;

import java.util.List;
import java.util.Optional;

/**
 * System Config Service Interface
 * Manages system configuration settings
 *
 * @author Service Team
 * @since 1.0
 */
public interface SystemConfigService {

    /**
     * Get config value by key
     *
     * @param key config key
     * @return config value
     */
    String getConfig(String key);

    /**
     * Get config value by key with default
     *
     * @param key config key
     * @param defaultValue default value
     * @return config value or default
     */
    String getConfig(String key, String defaultValue);

    /**
     * Get config as integer
     *
     * @param key config key
     * @param defaultValue default value
     * @return config value or default
     */
    int getConfigAsInt(String key, int defaultValue);

    /**
     * Get config as boolean
     *
     * @param key config key
     * @param defaultValue default value
     * @return config value or default
     */
    boolean getConfigAsBoolean(String key, boolean defaultValue);

    /**
     * Set config value
     *
     * @param key config key
     * @param value config value
     */
    void setConfig(String key, String value);

    /**
     * Set config with description
     *
     * @param key config key
     * @param value config value
     * @param description description
     * @param category category
     */
    void setConfig(String key, String value, String description, SystemConfig.ConfigCategory category);

    /**
     * Get all configs by category
     *
     * @param category config category
     * @return list of configs
     */
    List<SystemConfig> getConfigsByCategory(SystemConfig.ConfigCategory category);

    /**
     * Get all configs
     *
     * @return list of configs
     */
    List<SystemConfig> getAllConfigs();

    /**
     * Delete config
     *
     * @param key config key
     */
    void deleteConfig(String key);

    /**
     * Initialize default configs
     */
    void initializeDefaultConfigs();
}
