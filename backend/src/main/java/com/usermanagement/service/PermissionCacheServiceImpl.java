package com.usermanagement.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Permission Cache Service Implementation
 * Handles caching of user permissions in Redis for improved performance
 *
 * Uses Redis data structures:
 * - user:permissions:{userId} -> Set<String> (permission codes)
 * - user:roles:{userId} -> Set<String> (role codes)
 *
 * Cache Aside Pattern:
 * - Read: Check cache first, if miss load from DB and cache
 * - Write: Update DB, then invalidate cache
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class PermissionCacheServiceImpl implements PermissionCacheService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionCacheServiceImpl.class);

    // Redis key prefixes
    private static final String KEY_PREFIX_USER_PERMISSIONS = "user:permissions:";
    private static final String KEY_PREFIX_USER_ROLES = "user:roles:";

    // Cache TTL: 15 minutes
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoleService roleService;

    public PermissionCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                      @Lazy RoleService roleService) {
        this.redisTemplate = redisTemplate;
        this.roleService = roleService;
    }

    @Override
    public void cacheUserPermissions(UUID userId, Set<String> permissions) {
        if (userId == null || permissions == null) {
            return;
        }

        String key = buildPermissionsKey(userId);
        try {
            // Delete existing set first
            redisTemplate.delete(key);

            // Add all permissions
            if (!permissions.isEmpty()) {
                redisTemplate.opsForSet().add(key, permissions.toArray());
            }

            // Set expiration
            redisTemplate.expire(key, CACHE_TTL);

            logger.debug("Cached {} permissions for user: {}", permissions.size(), userId);
        } catch (Exception e) {
            logger.error("Failed to cache permissions for user: {}", userId, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getUserPermissions(UUID userId) {
        if (userId == null) {
            return new HashSet<>();
        }

        String key = buildPermissionsKey(userId);
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            if (members == null || members.isEmpty()) {
                return null; // Cache miss
            }

            Set<String> permissions = members.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());

            logger.debug("Cache hit for user permissions: {} permissions", permissions.size());
            return permissions;
        } catch (Exception e) {
            logger.error("Failed to get cached permissions for user: {}", userId, e);
            return null;
        }
    }

    @Override
    public void evictUserPermissions(UUID userId) {
        if (userId == null) {
            return;
        }

        String key = buildPermissionsKey(userId);
        try {
            redisTemplate.delete(key);
            logger.debug("Evicted permissions cache for user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to evict permissions cache for user: {}", userId, e);
        }
    }

    @Override
    public void evictAllPermissions() {
        try {
            // Find and delete all permission cache keys
            Set<String> keys = redisTemplate.keys(KEY_PREFIX_USER_PERMISSIONS + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Evicted all permissions cache: {} keys", keys.size());
            }
        } catch (Exception e) {
            logger.error("Failed to evict all permissions cache", e);
        }
    }

    @Override
    public boolean hasPermission(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }

        // Try cache first
        Set<String> permissions = getUserPermissions(userId);

        // Cache miss - load from database
        if (permissions == null) {
            permissions = loadAndCacheUserPermissions(userId);
        }

        return permissions != null && permissions.contains(permissionCode);
    }

    @Override
    public boolean hasAnyPermission(UUID userId, String... permissionCodes) {
        if (userId == null || permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }

        Set<String> permissions = getUserPermissions(userId);

        // Cache miss - load from database
        if (permissions == null) {
            permissions = loadAndCacheUserPermissions(userId);
        }

        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return Arrays.stream(permissionCodes)
                .anyMatch(permissions::contains);
    }

    @Override
    public boolean hasAllPermissions(UUID userId, String... permissionCodes) {
        if (userId == null || permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }

        Set<String> permissions = getUserPermissions(userId);

        // Cache miss - load from database
        if (permissions == null) {
            permissions = loadAndCacheUserPermissions(userId);
        }

        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return Arrays.stream(permissionCodes)
                .allMatch(permissions::contains);
    }

    @Override
    public void cacheUserRoles(UUID userId, Set<String> roleCodes) {
        if (userId == null || roleCodes == null) {
            return;
        }

        String key = buildRolesKey(userId);
        try {
            // Delete existing set first
            redisTemplate.delete(key);

            // Add all roles
            if (!roleCodes.isEmpty()) {
                redisTemplate.opsForSet().add(key, roleCodes.toArray());
            }

            // Set expiration
            redisTemplate.expire(key, CACHE_TTL);

            logger.debug("Cached {} roles for user: {}", roleCodes.size(), userId);
        } catch (Exception e) {
            logger.error("Failed to cache roles for user: {}", userId, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getUserRoles(UUID userId) {
        if (userId == null) {
            return new HashSet<>();
        }

        String key = buildRolesKey(userId);
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            if (members == null || members.isEmpty()) {
                return null; // Cache miss
            }

            return members.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Failed to get cached roles for user: {}", userId, e);
            return null;
        }
    }

    @Override
    public boolean hasRole(UUID userId, String roleCode) {
        if (userId == null || roleCode == null) {
            return false;
        }

        // Try cache first
        Set<String> roles = getUserRoles(userId);

        // Cache miss - load from database
        if (roles == null) {
            List<String> roleCodes = roleService.getRoleCodesByUserId(userId);
            roles = new HashSet<>(roleCodes);
            cacheUserRoles(userId, roles);
        }

        return roles.contains(roleCode);
    }

    @Override
    public void evictUserRoles(UUID userId) {
        if (userId == null) {
            return;
        }

        String key = buildRolesKey(userId);
        try {
            redisTemplate.delete(key);
            logger.debug("Evicted roles cache for user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to evict roles cache for user: {}", userId, e);
        }
    }

    @Override
    public Set<String> loadAndCacheUserPermissions(UUID userId) {
        logger.debug("Loading permissions from database for user: {}", userId);

        // Get user's roles
        List<String> roleCodes = roleService.getRoleCodesByUserId(userId);
        cacheUserRoles(userId, new HashSet<>(roleCodes));

        // Get user's roles DTOs and their permissions
        Set<String> allPermissions = new HashSet<>();
        var roleDTOs = roleService.getRolesByUserId(userId);

        // For each role, get its permissions
        for (var roleDTO : roleDTOs) {
            if (roleDTO.getId() != null) {
                List<String> rolePermissions = roleService.getRolePermissionCodes(roleDTO.getId());
                allPermissions.addAll(rolePermissions);
            }
        }

        // Cache the combined permissions
        cacheUserPermissions(userId, allPermissions);

        logger.debug("Loaded and cached {} permissions for user: {}", allPermissions.size(), userId);
        return allPermissions;
    }

    /**
     * Build Redis key for user permissions
     */
    private String buildPermissionsKey(UUID userId) {
        return KEY_PREFIX_USER_PERMISSIONS + userId.toString();
    }

    /**
     * Build Redis key for user roles
     */
    private String buildRolesKey(UUID userId) {
        return KEY_PREFIX_USER_ROLES + userId.toString();
    }
}
