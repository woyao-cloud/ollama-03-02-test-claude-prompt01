package com.usermanagement.service;

import com.usermanagement.domain.entity.SystemConfig;
import com.usermanagement.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * System Config Service Implementation
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigServiceImpl(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfig(String key) {
        return systemConfigRepository.findByKey(key)
                .map(SystemConfig::getValue)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfigAsInt(String key, int defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getConfigAsBoolean(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    @Transactional
    public void setConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findByKey(key)
                .orElse(new SystemConfig());
        config.setKey(key);
        config.setValue(value);
        systemConfigRepository.save(config);
        logger.debug("Set config: {} = {}", key, value);
    }

    @Override
    @Transactional
    public void setConfig(String key, String value, String description, SystemConfig.ConfigCategory category) {
        SystemConfig config = systemConfigRepository.findByKey(key)
                .orElse(new SystemConfig());
        config.setKey(key);
        config.setValue(value);
        config.setDescription(description);
        config.setCategory(category);
        systemConfigRepository.save(config);
        logger.debug("Set config: {} = {} (category: {})", key, value, category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> getConfigsByCategory(SystemConfig.ConfigCategory category) {
        return systemConfigRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteConfig(String key) {
        systemConfigRepository.deleteByKey(key);
        logger.debug("Deleted config: {}", key);
    }

    @Override
    @Transactional
    public void initializeDefaultConfigs() {
        // Password policy configs
        setDefaultConfig("password.minLength", "8", "Minimum password length", SystemConfig.ConfigCategory.PASSWORD);
        setDefaultConfig("password.requireUppercase", "true", "Require uppercase letter", SystemConfig.ConfigCategory.PASSWORD);
        setDefaultConfig("password.requireLowercase", "true", "Require lowercase letter", SystemConfig.ConfigCategory.PASSWORD);
        setDefaultConfig("password.requireDigit", "true", "Require digit", SystemConfig.ConfigCategory.PASSWORD);
        setDefaultConfig("password.requireSpecialChar", "true", "Require special character", SystemConfig.ConfigCategory.PASSWORD);
        setDefaultConfig("password.expiryDays", "90", "Password expiry days", SystemConfig.ConfigCategory.PASSWORD);

        // Login security configs
        setDefaultConfig("login.maxFailedAttempts", "5", "Maximum failed login attempts before lock", SystemConfig.ConfigCategory.SECURITY);
        setDefaultConfig("login.lockDurationMinutes", "30", "Account lock duration in minutes", SystemConfig.ConfigCategory.SECURITY);

        // Session configs
        setDefaultConfig("session.maxConcurrentSessions", "5", "Maximum concurrent sessions per user", SystemConfig.ConfigCategory.SESSION);
        setDefaultConfig("session.timeoutMinutes", "30", "Session timeout in minutes", SystemConfig.ConfigCategory.SESSION);

        logger.info("Default system configs initialized");
    }

    private void setDefaultConfig(String key, String value, String description, SystemConfig.ConfigCategory category) {
        if (!systemConfigRepository.existsByKey(key)) {
            SystemConfig config = new SystemConfig(key, value, description, category);
            config.setIsSystem(true);
            systemConfigRepository.save(config);
        }
    }
}
