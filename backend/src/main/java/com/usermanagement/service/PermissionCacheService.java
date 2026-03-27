package com.usermanagement.service;

import java.util.Set;
import java.util.UUID;

/**
 * Permission Cache Service Interface
 * Handles caching of user permissions in Redis for improved performance
 *
 * @author Service Team
 * @since 1.0
 */
public interface PermissionCacheService {

    /**
     * Cache user permissions in Redis
     *
     * @param userId user ID
     * @param permissions set of permission codes
     */
    void cacheUserPermissions(UUID userId, Set<String> permissions);

    /**
     * Get cached user permissions from Redis
     *
     * @param userId user ID
     * @return set of permission codes, or null if not cached
     */
    Set<String> getUserPermissions(UUID userId);

    /**
     * Evict user permissions from cache
     *
     * @param userId user ID
     */
    void evictUserPermissions(UUID userId);

    /**
     * Evict all user permissions from cache
     */
    void evictAllPermissions();

    /**
     * Check if user has specific permission (using cache)
     *
     * @param userId user ID
     * @param permissionCode permission code to check
     * @return true if user has permission
     */
    boolean hasPermission(UUID userId, String permissionCode);

    /**
     * Check if user has any of the specified permissions
     *
     * @param userId user ID
     * @param permissionCodes permission codes to check
     * @return true if user has any of the permissions
     */
    boolean hasAnyPermission(UUID userId, String... permissionCodes);

    /**
     * Check if user has all of the specified permissions
     *
     * @param userId user ID
     * @param permissionCodes permission codes to check
     * @return true if user has all permissions
     */
    boolean hasAllPermissions(UUID userId, String... permissionCodes);

    /**
     * Cache user roles in Redis
     *
     * @param userId user ID
     * @param roleCodes set of role codes
     */
    void cacheUserRoles(UUID userId, Set<String> roleCodes);

    /**
     * Get cached user roles from Redis
     *
     * @param userId user ID
     * @return set of role codes, or null if not cached
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * Check if user has specific role
     *
     * @param userId user ID
     * @param roleCode role code to check
     * @return true if user has role
     */
    boolean hasRole(UUID userId, String roleCode);

    /**
     * Evict user roles from cache
     *
     * @param userId user ID
     */
    void evictUserRoles(UUID userId);

    /**
     * Load and cache all permissions for a user
     *
     * @param userId user ID
     * @return set of permission codes
     */
    Set<String> loadAndCacheUserPermissions(UUID userId);
}
