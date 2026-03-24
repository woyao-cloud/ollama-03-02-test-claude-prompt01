package com.usermanagement.service;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.domain.entity.RolePermission;
import com.usermanagement.repository.PermissionRepository;
import com.usermanagement.repository.RolePermissionRepository;
import com.usermanagement.service.dto.CreatePermissionRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.PermissionTreeDTO;
import com.usermanagement.service.dto.UpdatePermissionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Permission Service Implementation
 * Handles permission management and tree structure
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionCacheService permissionCacheService;

    public PermissionServiceImpl(PermissionRepository permissionRepository,
                                 RolePermissionRepository rolePermissionRepository,
                                 PermissionCacheService permissionCacheService) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionCacheService = permissionCacheService;
    }

    @Override
    @Transactional
    public PermissionDTO createPermission(CreatePermissionRequest request) {
        logger.info("Creating permission with code: {}", request.getCode());

        // Validate code uniqueness
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Permission code already exists: " + request.getCode());
        }

        // Create permission
        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setCode(request.getCode());
        permission.setType(request.getType());
        permission.setResource(request.getResource());
        permission.setAction(request.getAction());
        permission.setIcon(request.getIcon());
        permission.setRoute(request.getRoute());
        permission.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        permission.setStatus(Permission.PermissionStatus.ACTIVE);

        // Set parent if provided
        if (request.getParentId() != null) {
            Permission parent = permissionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent permission not found: " + request.getParentId()));
            permission.setParent(parent);
        }

        Permission saved = permissionRepository.save(permission);
        logger.info("Permission created with ID: {}", saved.getId());

        // Clear all permissions cache
        permissionCacheService.evictAllPermissions();

        return PermissionDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public PermissionDTO updatePermission(UUID id, UpdatePermissionRequest request) {
        logger.info("Updating permission with ID: {}", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));

        // Update fields
        if (request.getName() != null) {
            permission.setName(request.getName());
        }
        if (request.getType() != null) {
            permission.setType(request.getType());
        }
        if (request.getResource() != null) {
            permission.setResource(request.getResource());
        }
        if (request.getAction() != null) {
            permission.setAction(request.getAction());
        }
        if (request.getIcon() != null) {
            permission.setIcon(request.getIcon());
        }
        if (request.getRoute() != null) {
            permission.setRoute(request.getRoute());
        }
        if (request.getSortOrder() != null) {
            permission.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            permission.setStatus(request.getStatus());
        }

        // Update parent if provided
        if (request.getParentId() != null) {
            // Prevent circular reference
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Permission cannot be its own parent");
            }
            Permission parent = permissionRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent permission not found: " + request.getParentId()));
            permission.setParent(parent);
        } else if (request.getParentId() == null && permission.getParent() != null) {
            // Remove parent if explicitly set to null
            permission.setParent(null);
        }

        Permission updated = permissionRepository.save(permission);
        logger.info("Permission updated: {}", id);

        // Clear caches
        permissionCacheService.evictAllPermissions();

        return PermissionDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        logger.info("Deleting permission with ID: {}", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));

        // Check if permission is assigned to any role
        long roleCount = rolePermissionRepository.countByPermissionId(id);
        if (roleCount > 0) {
            throw new IllegalStateException(
                "Cannot delete permission: it is assigned to " + roleCount + " role(s)"
            );
        }

        // Check if permission has children
        if (permission.getChildren() != null && !permission.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete permission: it has child permissions");
        }

        // Soft delete
        permission.setDeletedAt(Instant.now());
        permissionRepository.save(permission);

        // Clear caches
        permissionCacheService.evictAllPermissions();

        logger.info("Permission soft deleted: {}", id);
    }

    @Override
    public PermissionDTO getPermissionById(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
        return PermissionDTO.fromEntity(permission);
    }

    @Override
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getAllActivePermissions() {
        return permissionRepository.findByStatus(Permission.PermissionStatus.ACTIVE).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PermissionDTO> getPermissions(Pageable pageable) {
        return permissionRepository.findByDeletedAtIsNull(pageable)
                .map(PermissionDTO::fromEntity);
    }

    @Override
    public List<PermissionTreeDTO> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findByStatus(Permission.PermissionStatus.ACTIVE);

        // Filter out deleted permissions
        allPermissions = allPermissions.stream()
                .filter(p -> p.getDeletedAt() == null)
                .collect(Collectors.toList());

        // Build tree
        Map<UUID, PermissionTreeDTO> dtoMap = new HashMap<>();
        List<PermissionTreeDTO> roots = new ArrayList<>();

        // First pass: create DTOs
        for (Permission permission : allPermissions) {
            PermissionTreeDTO dto = PermissionTreeDTO.fromEntity(permission);
            dtoMap.put(permission.getId(), dto);
        }

        // Second pass: build hierarchy
        for (Permission permission : allPermissions) {
            PermissionTreeDTO dto = dtoMap.get(permission.getId());
            if (permission.getParent() == null) {
                roots.add(dto);
            } else {
                PermissionTreeDTO parentDto = dtoMap.get(permission.getParent().getId());
                if (parentDto != null) {
                    parentDto.addChild(dto);
                } else {
                    // Parent not found, treat as root
                    roots.add(dto);
                }
            }
        }

        // Sort by sortOrder
        roots.sort((a, b) -> {
            int sortCompare = Integer.compare(
                    a.getSortOrder() != null ? a.getSortOrder() : 0,
                    b.getSortOrder() != null ? b.getSortOrder() : 0
            );
            if (sortCompare != 0) return sortCompare;
            return a.getName().compareTo(b.getName());
        });

        return roots;
    }

    @Override
    public List<PermissionDTO> getPermissionsByType(Permission.PermissionType type) {
        return permissionRepository.findByTypeAndStatus(type, Permission.PermissionStatus.ACTIVE).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getPermissionsByRoleId(UUID roleId) {
        List<Permission> permissions = permissionRepository.findByRoleId(roleId);
        return permissions.stream()
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getMenuPermissions() {
        return permissionRepository.findMenuPermissions().stream()
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PermissionDTO> findByCode(String code) {
        return permissionRepository.findByCode(code)
                .map(PermissionDTO::fromEntity);
    }

    @Override
    public boolean existsByCode(String code) {
        return permissionRepository.existsByCode(code);
    }

    @Override
    public List<PermissionDTO> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PermissionDTO> findByResourceAndAction(String resource, String action) {
        return permissionRepository.findByResourceAndAction(resource, action)
                .map(PermissionDTO::fromEntity);
    }

    @Override
    @Transactional
    public void initializeDefaultPermissions() {
        logger.info("Initializing default permissions...");

        // User management permissions
        createPermissionIfNotExists("user:menu:view", "User Management Menu", Permission.PermissionType.MENU, "user", "menu:view", 1);
        createPermissionIfNotExists("user:create", "Create User", Permission.PermissionType.ACTION, "user", "create", 2);
        createPermissionIfNotExists("user:read", "View User", Permission.PermissionType.ACTION, "user", "read", 3);
        createPermissionIfNotExists("user:update", "Update User", Permission.PermissionType.ACTION, "user", "update", 4);
        createPermissionIfNotExists("user:delete", "Delete User", Permission.PermissionType.ACTION, "user", "delete", 5);

        // Role management permissions
        createPermissionIfNotExists("role:menu:view", "Role Management Menu", Permission.PermissionType.MENU, "role", "menu:view", 10);
        createPermissionIfNotExists("role:create", "Create Role", Permission.PermissionType.ACTION, "role", "create", 11);
        createPermissionIfNotExists("role:read", "View Role", Permission.PermissionType.ACTION, "role", "read", 12);
        createPermissionIfNotExists("role:update", "Update Role", Permission.PermissionType.ACTION, "role", "update", 13);
        createPermissionIfNotExists("role:delete", "Delete Role", Permission.PermissionType.ACTION, "role", "delete", 14);
        createPermissionIfNotExists("role:assign", "Assign Permissions", Permission.PermissionType.ACTION, "role", "assign", 15);

        // Permission management permissions
        createPermissionIfNotExists("permission:menu:view", "Permission Management Menu", Permission.PermissionType.MENU, "permission", "menu:view", 20);
        createPermissionIfNotExists("permission:create", "Create Permission", Permission.PermissionType.ACTION, "permission", "create", 21);
        createPermissionIfNotExists("permission:read", "View Permission", Permission.PermissionType.ACTION, "permission", "read", 22);
        createPermissionIfNotExists("permission:update", "Update Permission", Permission.PermissionType.ACTION, "permission", "update", 23);
        createPermissionIfNotExists("permission:delete", "Delete Permission", Permission.PermissionType.ACTION, "permission", "delete", 24);

        // System settings permissions
        createPermissionIfNotExists("system:menu:view", "System Settings Menu", Permission.PermissionType.MENU, "system", "menu:view", 30);
        createPermissionIfNotExists("system:config", "System Configuration", Permission.PermissionType.ACTION, "system", "config", 31);

        logger.info("Default permissions initialized");
    }

    private void createPermissionIfNotExists(String code, String name, Permission.PermissionType type,
                                             String resource, String action, int sortOrder) {
        if (!permissionRepository.existsByCode(code)) {
            Permission permission = new Permission();
            permission.setCode(code);
            permission.setName(name);
            permission.setType(type);
            permission.setResource(resource);
            permission.setAction(action);
            permission.setSortOrder(sortOrder);
            permission.setStatus(Permission.PermissionStatus.ACTIVE);
            permissionRepository.save(permission);
            logger.debug("Created permission: {}", code);
        }
    }
}
