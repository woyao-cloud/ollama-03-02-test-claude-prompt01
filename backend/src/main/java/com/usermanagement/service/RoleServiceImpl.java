package com.usermanagement.service;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.RolePermission;
import com.usermanagement.domain.entity.UserRole;
import com.usermanagement.repository.PermissionRepository;
import com.usermanagement.repository.RolePermissionRepository;
import com.usermanagement.repository.RoleRepository;
import com.usermanagement.repository.UserRoleRepository;
import com.usermanagement.service.dto.CreateRoleRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.RoleDTO;
import com.usermanagement.service.dto.RoleQueryRequest;
import com.usermanagement.service.dto.UpdateRoleRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Role Service Implementation
 * Handles role management, permission assignment, and data scope configuration
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionCacheService permissionCacheService;

    public RoleServiceImpl(RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository,
                          UserRoleRepository userRoleRepository,
                          PermissionCacheService permissionCacheService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionCacheService = permissionCacheService;
    }

    @Override
    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        logger.info("Creating role with code: {}", request.getCode());

        // Validate code uniqueness
        if (roleRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Role code already exists: " + request.getCode());
        }

        // Validate name uniqueness
        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role name already exists: " + request.getName());
        }

        // Create role
        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setDataScope(request.getDataScope());
        role.setStatus(Role.RoleStatus.ACTIVE);
        role.setIsSystem(false);

        Role savedRole = roleRepository.save(role);
        logger.info("Role created with ID: {}", savedRole.getId());

        // Assign permissions if provided
        if (request.getPermissionCodes() != null && !request.getPermissionCodes().isEmpty()) {
            assignPermissionsByCodes(savedRole.getId(), request.getPermissionCodes());
        }

        return RoleDTO.fromEntity(savedRole);
    }

    @Override
    @Transactional
    public RoleDTO updateRole(UUID id, UpdateRoleRequest request) {
        logger.info("Updating role with ID: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        // Check if system role
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getCode());
        }

        // Validate name uniqueness (if changed)
        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role name already exists: " + request.getName());
        }

        // Update fields
        role.setName(request.getName());
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getDataScope() != null) {
            role.setDataScope(request.getDataScope());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }

        Role updatedRole = roleRepository.save(role);
        logger.info("Role updated: {}", id);

        // Update permissions if provided
        if (request.getPermissionCodes() != null) {
            assignPermissionsByCodes(id, request.getPermissionCodes());
        }

        // Clear cache for users with this role
        clearCacheForRoleUsers(id);

        return RoleDTO.fromEntity(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        logger.info("Deleting role with ID: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        // Check if system role
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalStateException("Cannot delete system role: " + role.getCode());
        }

        // Check if role is assigned to any user
        List<UserRole> userRoles = userRoleRepository.findByRoleId(id);
        if (!userRoles.isEmpty()) {
            throw new IllegalStateException(
                "Cannot delete role: it is assigned to " + userRoles.size() + " user(s)"
            );
        }

        // Clear cache for this role
        clearCacheForRoleUsers(id);

        // Soft delete
        role.setDeletedAt(Instant.now());
        roleRepository.save(role);

        logger.info("Role soft deleted: {}", id);
    }

    @Override
    public RoleDTO getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        return RoleDTO.fromEntity(role);
    }

    @Override
    public RoleDTO getRoleByIdWithPermissions(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        RoleDTO dto = RoleDTO.fromEntity(role);

        // Load permissions
        List<PermissionDTO> permissions = getRolePermissions(id);
        dto.setPermissions(permissions);
        dto.setPermissionCount(permissions.size());

        return dto;
    }

    @Override
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAllActive().stream()
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoleDTO> getAllActiveRoles() {
        return roleRepository.findByStatus(Role.RoleStatus.ACTIVE).stream()
                .filter(role -> !role.isDeleted())
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<RoleDTO> getRoles(RoleQueryRequest query) {
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(query.getSortDirection()) ?
                        Sort.Direction.DESC : Sort.Direction.ASC,
                query.getSortBy()
        );

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        // Build query based on filters
        Page<Role> rolePage;
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            // Keyword search on name and code
            rolePage = roleRepository.findAllActive(pageable);
        } else if (query.getStatus() != null) {
            rolePage = roleRepository.findByStatus(query.getStatus(), pageable);
        } else if (query.getDataScope() != null) {
            List<Role> roles = roleRepository.findByDataScope(query.getDataScope());
            // Convert to page manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), roles.size());
            List<RoleDTO> dtoList = roles.subList(start, end).stream()
                    .map(RoleDTO::fromEntity)
                    .collect(Collectors.toList());
            return new PageImpl<>(dtoList, pageable, roles.size());
        } else {
            rolePage = roleRepository.findAllActive(pageable);
        }

        return rolePage.map(RoleDTO::fromEntity);
    }

    @Override
    @Transactional
    public void assignPermissions(UUID roleId, List<UUID> permissionIds) {
        logger.info("Assigning permissions to role {}: {}", roleId, permissionIds);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Remove existing permissions
        rolePermissionRepository.deleteByRoleId(roleId);

        // Add new permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (UUID permissionId : permissionIds) {
                Permission permission = permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

                RolePermission rp = new RolePermission();
                rp.setRole(role);
                rp.setPermission(permission);
                rolePermissionRepository.save(rp);
            }
        }

        // Clear cache for users with this role
        clearCacheForRoleUsers(roleId);

        logger.info("Permissions assigned to role: {}", roleId);
    }

    @Override
    @Transactional
    public void assignPermissionsByCodes(UUID roleId, List<String> permissionCodes) {
        logger.info("Assigning permissions by codes to role {}: {}", roleId, permissionCodes);

        if (permissionCodes == null || permissionCodes.isEmpty()) {
            assignPermissions(roleId, new ArrayList<>());
            return;
        }

        List<UUID> permissionIds = new ArrayList<>();
        for (String code : permissionCodes) {
            permissionRepository.findByCode(code)
                    .ifPresent(p -> permissionIds.add(p.getId()));
        }

        assignPermissions(roleId, permissionIds);
    }

    @Override
    public List<PermissionDTO> getRolePermissions(UUID roleId) {
        List<Permission> permissions = permissionRepository.findByRoleId(roleId);
        return permissions.stream()
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRolePermissionCodes(UUID roleId) {
        List<Permission> permissions = permissionRepository.findByRoleId(roleId);
        return permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleDTO updateDataScope(UUID roleId, Role.DataScope dataScope) {
        logger.info("Updating data scope for role {} to {}", roleId, dataScope);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        role.setDataScope(dataScope);
        Role updated = roleRepository.save(role);

        // Clear cache
        clearCacheForRoleUsers(roleId);

        return RoleDTO.fromEntity(updated);
    }

    @Override
    public boolean existsByCode(String code) {
        return roleRepository.existsByCode(code);
    }

    @Override
    public List<RoleDTO> getRolesByUserId(UUID userId) {
        List<Role> roles = roleRepository.findByUserId(userId);
        return roles.stream()
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleCodesByUserId(UUID userId) {
        List<Role> roles = roleRepository.findByUserId(userId);
        return roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDTO getRoleByCode(String code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + code));
        return RoleDTO.fromEntity(role);
    }

    /**
     * Clear permission cache for all users with the given role
     */
    private void clearCacheForRoleUsers(UUID roleId) {
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        for (UserRole userRole : userRoles) {
            permissionCacheService.evictUserPermissions(userRole.getUser().getId());
        }
        logger.debug("Cleared cache for {} users with role {}", userRoles.size(), roleId);
    }
}
