package com.usermanagement.service;

import com.usermanagement.domain.entity.Department;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.User;
import com.usermanagement.domain.entity.UserRole;
import com.usermanagement.repository.DepartmentRepository;
import com.usermanagement.repository.RoleRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.repository.UserRoleRepository;
import com.usermanagement.repository.spec.UserSpecification;
import com.usermanagement.service.dto.CreateUserRequest;
import com.usermanagement.service.dto.UpdateProfileRequest;
import com.usermanagement.service.dto.UpdateUserRequest;
import com.usermanagement.service.dto.UserDTO;
import com.usermanagement.service.dto.UserQueryRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Service Implementation
 * Implements user management business logic
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRoleRepository userRoleRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                          RoleRepository roleRepository,
                          DepartmentRepository departmentRepository,
                          UserRoleRepository userRoleRepository,
                          SessionService sessionService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDTO createUser(CreateUserRequest request) {
        logger.info("Creating new user with email: {}", request.getEmail());

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create user entity
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setStatus(User.UserStatus.PENDING);
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);

        // Encode password with BCrypt (strength=12 configured in SecurityConfig)
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChangedAt(Instant.now());
        }

        // Set department if provided
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));
            user.setDepartment(department);
        }

        // Save user first
        User savedUser = userRepository.save(user);

        // Assign roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignRoles(savedUser.getId(), request.getRoleIds());
        }

        logger.info("User created successfully with ID: {}", savedUser.getId());
        return convertToDTO(savedUser);
    }

    @Override
    public UserDTO updateUser(UUID id, UpdateUserRequest request) {
        logger.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        // Check if user is deleted
        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted user: " + id);
        }

        // Update email if changed
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail().toLowerCase().trim());
        }

        // Update other fields
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Update department if provided
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));
            user.setDepartment(department);
        } else if (request.getDepartmentId() == null && user.getDepartment() != null) {
            // Explicitly set to null (remove department)
            user.setDepartment(null);
        }

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", id);
        return convertToDTO(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        logger.info("Soft deleting user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("User is already deleted: " + id);
        }

        // Invalidate all user sessions
        sessionService.invalidateAllUserSessions(id);

        // Soft delete
        user.softDelete();
        userRepository.save(user);

        logger.info("User soft deleted successfully: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("User not found: " + id);
        }

        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsers(UserQueryRequest query) {
        query.normalize();

        // Create specification for dynamic query
        Specification<User> spec = UserSpecification.withQuery(query);

        // Create pageable
        Sort sort = Sort.by(
                Sort.Direction.fromString(query.getSortDirection()),
                query.getSortBy()
        );
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        // Execute query
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // Convert to DTOs
        return userPage.map(this::convertToDTO);
    }

    @Override
    public void updateStatus(UUID id, String statusStr) {
        logger.info("Updating user status for ID: {} to {}", id, statusStr);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot update status of deleted user: " + id);
        }

        User.UserStatus newStatus;
        try {
            newStatus = User.UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }

        user.setStatus(newStatus);

        // If unlocking, clear lock info
        if (newStatus == User.UserStatus.ACTIVE && user.getLockedUntil() != null) {
            user.unlockAccount();
        }

        userRepository.save(user);
        logger.info("User status updated successfully: {} -> {}", id, newStatus);
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

        // Clear user permissions cache
        sessionService.clearUserPermissionsCache(userId);

        logger.info("Roles assigned successfully to user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getProfile(UUID id) {
        // Profile is essentially the same as getUserById, but can be extended
        return getUserById(id);
    }

    @Override
    public UserDTO updateProfile(UUID id, UpdateProfileRequest request) {
        logger.info("Updating profile for user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot update profile of deleted user: " + id);
        }

        // Update basic info
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Update password if provided
        if (request.isPasswordChange()) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new IllegalArgumentException("Current password is required to change password");
            }

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }

            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setPasswordChangedAt(Instant.now());

            // Optionally: invalidate all sessions except current
            // sessionService.invalidateAllUserSessionsExceptCurrent(id);
        }

        User updatedUser = userRepository.save(user);
        logger.info("Profile updated successfully for user: {}", id);
        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return !userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    public void unlockUser(UUID id) {
        logger.info("Unlocking user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Cannot unlock deleted user: " + id);
        }

        user.unlockAccount();
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        logger.info("User unlocked successfully: {}", id);
    }

    /**
     * Convert User entity to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        // Department info
        if (user.getDepartment() != null) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getName());
        }

        // Roles info
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            List<UserDTO.RoleInfo> roleInfos = user.getUserRoles().stream()
                    .map(ur -> new UserDTO.RoleInfo(
                            ur.getRole().getId(),
                            ur.getRole().getName(),
                            ur.getRole().getCode()
                    ))
                    .collect(Collectors.toList());
            dto.setRoles(roleInfos);
        } else {
            dto.setRoles(new ArrayList<>());
        }

        return dto;
    }
}
