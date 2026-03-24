package com.usermanagement.service;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.service.dto.CreatePermissionRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.PermissionTreeDTO;
import com.usermanagement.service.dto.UpdatePermissionRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Permission Service Interface
 * Handles permission management and tree structure
 *
 * @author Service Team
 * @since 1.0
 */
public interface PermissionService {

    /**
     * Create a new permission
     *
     * @param request permission creation request
     * @return created permission DTO
     */
    PermissionDTO createPermission(CreatePermissionRequest request);

    /**
     * Update an existing permission
     *
     * @param id permission ID
     * @param request permission update request
     * @return updated permission DTO
     */
    PermissionDTO updatePermission(UUID id, UpdatePermissionRequest request);

    /**
     * Delete a permission
     *
     * @param id permission ID
     */
    void deletePermission(UUID id);

    /**
     * Get permission by ID
     *
     * @param id permission ID
     * @return permission DTO
     */
    PermissionDTO getPermissionById(UUID id);

    /**
     * Get all permissions
     *
     * @return list of permission DTOs
     */
    List<PermissionDTO> getAllPermissions();

    /**
     * Get all active permissions
     *
     * @return list of active permission DTOs
     */
    List<PermissionDTO> getAllActivePermissions();

    /**
     * Get permissions with pagination
     *
     * @param pageable pagination parameters
     * @return paginated permission DTOs
     */
    Page<PermissionDTO> getPermissions(Pageable pageable);

    /**
     * Get permission tree
     *
     * @return tree structure of permissions
     */
    List<PermissionTreeDTO> getPermissionTree();

    /**
     * Get permissions by type
     *
     * @param type permission type
     * @return list of permission DTOs
     */
    List<PermissionDTO> getPermissionsByType(Permission.PermissionType type);

    /**
     * Get permissions by role ID
     *
     * @param roleId role ID
     * @return list of permission DTOs
     */
    List<PermissionDTO> getPermissionsByRoleId(UUID roleId);

    /**
     * Get menu permissions
     *
     * @return list of menu permission DTOs
     */
    List<PermissionDTO> getMenuPermissions();

    /**
     * Find permission by code
     *
     * @param code permission code
     * @return optional permission DTO
     */
    Optional<PermissionDTO> findByCode(String code);

    /**
     * Check if permission exists by code
     *
     * @param code permission code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Get permissions by resource
     *
     * @param resource resource name
     * @return list of permission DTOs
     */
    List<PermissionDTO> getPermissionsByResource(String resource);

    /**
     * Find permission by resource and action
     *
     * @param resource resource name
     * @param action action name
     * @return optional permission DTO
     */
    Optional<PermissionDTO> findByResourceAndAction(String resource, String action);

    /**
     * Initialize default permissions
     * Should be called on application startup
     */
    void initializeDefaultPermissions();
}
