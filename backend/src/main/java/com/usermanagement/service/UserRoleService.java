package com.usermanagement.service;

import com.usermanagement.service.dto.RoleDTO;

import java.util.List;
import java.util.UUID;

/**
 * User Role Service Interface
 * Handles user role assignment and permission management
 *
 * @author Service Team
 * @since 1.0
 */
public interface UserRoleService {

    /**
     * Assign roles to user (replace existing roles)
     *
     * @param userId user ID
     * @param roleIds list of role IDs to assign
     */
    void assignRoles(UUID userId, List<UUID> roleIds);

    /**
     * Add a single role to user
     *
     * @param userId user ID
     * @param roleId role ID to add
     */
    void addRole(UUID userId, UUID roleId);

    /**
     * Remove a role from user
     *
     * @param userId user ID
     * @param roleId role ID to remove
     */
    void removeRole(UUID userId, UUID roleId);

    /**
     * Get user's roles
     *
     * @param userId user ID
     * @return list of role DTOs
     */
    List<RoleDTO> getUserRoles(UUID userId);

    /**
     * Get user's permission codes
     *
     * @param userId user ID
     * @return list of permission codes
     */
    List<String> getUserPermissions(UUID userId);

    /**
     * Check if user has specific role
     *
     * @param userId user ID
     * @param roleId role ID
     * @return true if user has the role
     */
    boolean hasRole(UUID userId, UUID roleId);

    /**
     * Check if user has specific role by code
     *
     * @param userId user ID
     * @param roleCode role code
     * @return true if user has the role
     */
    boolean hasRoleByCode(UUID userId, String roleCode);

    /**
     * Check if user has specific permission
     *
     * @param userId user ID
     * @param permissionCode permission code
     * @return true if user has the permission
     */
    boolean hasPermission(UUID userId, String permissionCode);

    /**
     * Clear user's role cache
     *
     * @param userId user ID
     */
    void clearUserRoleCache(UUID userId);
}
