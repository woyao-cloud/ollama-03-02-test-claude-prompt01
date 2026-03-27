package com.usermanagement.service;

import com.usermanagement.domain.entity.Role;
import com.usermanagement.service.dto.CreateRoleRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.RoleDTO;
import com.usermanagement.service.dto.RoleQueryRequest;
import com.usermanagement.service.dto.UpdateRoleRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Role Service Interface
 * Handles role management, permission assignment, and data scope configuration
 *
 * @author Service Team
 * @since 1.0
 */
public interface RoleService {

    /**
     * Create a new role
     *
     * @param request role creation request
     * @return created role DTO
     */
    RoleDTO createRole(CreateRoleRequest request);

    /**
     * Update an existing role
     *
     * @param id role ID
     * @param request role update request
     * @return updated role DTO
     */
    RoleDTO updateRole(UUID id, UpdateRoleRequest request);

    /**
     * Delete a role
     *
     * @param id role ID
     */
    void deleteRole(UUID id);

    /**
     * Get role by ID
     *
     * @param id role ID
     * @return role DTO
     */
    RoleDTO getRoleById(UUID id);

    /**
     * Get role by ID with permissions
     *
     * @param id role ID
     * @return role DTO with permissions
     */
    RoleDTO getRoleByIdWithPermissions(UUID id);

    /**
     * Get all roles
     *
     * @return list of role DTOs
     */
    List<RoleDTO> getAllRoles();

    /**
     * Get all active roles
     *
     * @return list of active role DTOs
     */
    List<RoleDTO> getAllActiveRoles();

    /**
     * Get roles with pagination and filters
     *
     * @param query query request
     * @return paginated role DTOs
     */
    Page<RoleDTO> getRoles(RoleQueryRequest query);

    /**
     * Assign permissions to a role
     *
     * @param roleId role ID
     * @param permissionIds list of permission IDs
     */
    void assignPermissions(UUID roleId, List<UUID> permissionIds);

    /**
     * Assign permissions by permission codes
     *
     * @param roleId role ID
     * @param permissionCodes list of permission codes
     */
    void assignPermissionsByCodes(UUID roleId, List<String> permissionCodes);

    /**
     * Get role permissions
     *
     * @param roleId role ID
     * @return list of permission DTOs
     */
    List<PermissionDTO> getRolePermissions(UUID roleId);

    /**
     * Get role permission codes
     *
     * @param roleId role ID
     * @return list of permission codes
     */
    List<String> getRolePermissionCodes(UUID roleId);

    /**
     * Update role data scope
     *
     * @param roleId role ID
     * @param dataScope data scope
     * @return updated role DTO
     */
    RoleDTO updateDataScope(UUID roleId, Role.DataScope dataScope);

    /**
     * Check if role exists by code
     *
     * @param code role code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Get roles by user ID
     *
     * @param userId user ID
     * @return list of role DTOs
     */
    List<RoleDTO> getRolesByUserId(UUID userId);

    /**
     * Get role codes by user ID
     *
     * @param userId user ID
     * @return list of role codes
     */
    List<String> getRoleCodesByUserId(UUID userId);

    /**
     * Get role by code
     *
     * @param code role code
     * @return role DTO
     */
    RoleDTO getRoleByCode(String code);
}
