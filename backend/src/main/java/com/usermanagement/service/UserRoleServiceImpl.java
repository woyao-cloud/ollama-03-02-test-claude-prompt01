package com.usermanagement.service;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.User;
import com.usermanagement.domain.entity.UserRole;
import com.usermanagement.repository.RoleRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.repository.UserRoleRepository;
import com.usermanagement.service.dto.RoleDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Role Service Implementation
 * Handles user role assignment and permission management
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

    private static final Logger logger = LoggerFactory.getLogger(UserRoleServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SessionService sessionService;

    public UserRoleServiceImpl(UserRepository userRepository,
                               RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository,
                               SessionService sessionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionService = sessionService;
    }

    @Override
    public void assignRoles(UUID userId, List<UUID> roleIds) {
        logger.info("Assigning roles to user: {}, roles: {}", userId, roleIds);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot assign roles to deleted user: " + userId);
        }

        // Remove existing roles
        userRoleRepository.deleteByUserId(userId);
        user.getUserRoles().clear();

        // Add new roles
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<UUID> uniqueRoleIds = new HashSet<>(roleIds);
            for (UUID roleId : uniqueRoleIds) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

                if (role.isDeleted()) {
                    throw new IllegalArgumentException("Cannot assign deleted role: " + roleId);
                }

                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                user.getUserRoles().add(userRole);
            }
            userRepository.save(user);
        }

        // Clear cache
        clearUserRoleCache(userId);

        // Log audit event (to be implemented with AOP)
        logger.info("Roles assigned successfully to user: {}", userId);
    }

    @Override
    public void addRole(UUID userId, UUID roleId) {
        logger.info("Adding role {} to user {}", roleId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot add role to deleted user: " + userId);
        }

        // Check if user already has this role
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            logger.debug("User {} already has role {}, skipping", userId, roleId);
            return;
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        if (role.isDeleted()) {
            throw new IllegalArgumentException("Cannot add deleted role: " + roleId);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);

        userRepository.save(user);

        // Clear cache
        clearUserRoleCache(userId);

        logger.info("Role {} added to user {}", roleId, userId);
    }

    @Override
    public void removeRole(UUID userId, UUID roleId) {
        logger.info("Removing role {} from user {}", roleId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot remove role from deleted user: " + userId);
        }

        // Check if user has this role
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            logger.debug("User {} does not have role {}, nothing to remove", userId, roleId);
            return;
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);

        // Clear cache
        clearUserRoleCache(userId);

        logger.info("Role {} removed from user {}", roleId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getUserRoles(UUID userId) {
        logger.debug("Getting roles for user: {}", userId);

        // Verify user exists and is not deleted
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("User is deleted: " + userId);
        }

        // Get roles from repository
        List<Role> roles = roleRepository.findByUserId(userId);

        // Convert to DTOs
        return roles.stream()
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPermissions(UUID userId) {
        logger.debug("Getting permissions for user: {}", userId);

        // Verify user exists and is not deleted
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("User is deleted: " + userId);
        }

        // Get all permissions from user's roles
        Set<String> permissionCodes = new HashSet<>();

        for (UserRole userRole : user.getUserRoles()) {
            Role role = userRole.getRole();
            if (role.isActive()) {
                for (var rolePermission : role.getRolePermissions()) {
                    Permission permission = rolePermission.getPermission();
                    if (permission.isActive()) {
                        permissionCodes.add(permission.getCode());
                    }
                }
            }
        }

        return new ArrayList<>(permissionCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, UUID roleId) {
        return userRoleRepository.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRoleByCode(UUID userId, String roleCode) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isDeleted()) {
            return false;
        }

        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode().equals(roleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String permissionCode) {
        List<String> permissions = getUserPermissions(userId);
        return permissions.contains(permissionCode);
    }

    @Override
    public void clearUserRoleCache(UUID userId) {
        logger.debug("Clearing role cache for user: {}", userId);
        sessionService.clearUserPermissionsCache(userId);
    }

    /**
     * Convert Role entity to RoleDTO
     */
    private RoleDTO convertToRoleDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setCode(role.getCode());
        dto.setDescription(role.getDescription());
        dto.setDataScope(role.getDataScope());
        dto.setStatus(role.getStatus());
        dto.setIsSystem(role.getIsSystem());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        // Get permission codes
        List<String> permissionCodes = role.getRolePermissions().stream()
                .filter(rp -> rp.getPermission().isActive())
                .map(rp -> rp.getPermission().getCode())
                .collect(Collectors.toList());
        dto.setPermissionCodes(permissionCodes);

        return dto;
    }
}
