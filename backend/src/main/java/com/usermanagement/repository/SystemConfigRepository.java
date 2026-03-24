package com.usermanagement.repository;

import com.usermanagement.domain.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * System Config Repository
 *
 * @author Repository Team
 * @since 1.0
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    /**
     * Find config by key
     *
     * @param key config key
     * @return optional config
     */
    Optional<SystemConfig> findByKey(String key);

    /**
     * Find configs by category
     *
     * @param category config category
     * @return list of configs
     */
    List<SystemConfig> findByCategory(SystemConfig.ConfigCategory category);

    /**
     * Check if config exists by key
     *
     * @param key config key
     * @return true if exists
     */
    boolean existsByKey(String key);

    /**
     * Delete config by key
     *
     * @param key config key
     */
    void deleteByKey(String key);
}
