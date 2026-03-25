package com.usermanagement.web.controller;

import com.usermanagement.security.SecurityUtils;
import com.usermanagement.service.UserService;
import com.usermanagement.service.dto.CreateUserRequest;
import com.usermanagement.service.dto.UpdateProfileRequest;
import com.usermanagement.service.dto.UpdateUserRequest;
import com.usermanagement.service.dto.UserDTO;
import com.usermanagement.service.dto.UserQueryRequest;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.AssignRolesRequest;
import com.usermanagement.web.dto.PageResponse;
import com.usermanagement.web.dto.UpdateUserStatusRequest;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User Controller
 * REST API endpoints for user management
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public UserController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all users with pagination and filters
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> getUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.debug("Getting users with filters");

        UserQueryRequest query = new UserQueryRequest();
        query.setEmail(email);
        query.setFirstName(firstName);
        query.setLastName(lastName);
        query.setPhone(phone);
        if (status != null && !status.isEmpty()) {
            try {
                query.setStatus(com.usermanagement.domain.entity.User.UserStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", status);
            }
        }
        query.setDepartmentId(departmentId);
        query.setRoleId(roleId);
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);

        Page<UserDTO> userPage = userService.getUsers(query);
        PageResponse<UserDTO> pageResponse = PageResponse.from(userPage);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", pageResponse));
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ') or hasAuthority('ADMIN') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable UUID id) {
        logger.debug("Getting user by ID: {}", id);

        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Create a new user
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody CreateUserRequest request) {
        logger.info("Creating new user with email: {}", request.getEmail());

        UserDTO createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", createdUser));
    }

    /**
     * Update an existing user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        logger.info("Updating user with ID: {}", id);

        UserDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    /**
     * Update user status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        logger.info("Updating user status for ID: {} to {}", id, request.getStatus());

        userService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", null));
    }

    /**
     * Delete a user (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        logger.info("Deleting user with ID: {}", id);

        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("User deleted successfully", null));
    }

    /**
     * Assign roles to user
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRoles(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest request) {
        logger.info("Assigning roles to user: {}, roles: {}", id, request.getRoleIds());

        userService.assignRoles(id, request.getRoleIds());
        return ResponseEntity.ok(ApiResponse.success("Roles assigned successfully", null));
    }

    /**
     * Get user profile
     */
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('USER_READ') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(@PathVariable UUID id) {
        logger.debug("Getting profile for user: {}", id);

        UserDTO profile = userService.getProfile(id);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    /**
     * Update user profile (self-service)
     */
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request) {
        logger.info("Updating profile for user: {}", id);

        UserDTO updatedProfile = userService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
    }

    /**
     * Unlock a locked user account
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable UUID id) {
        logger.info("Unlocking user: {}", id);

        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success("User unlocked successfully", null));
    }

    /**
     * Check if email is available
     */
    @GetMapping("/check-email")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(
            @RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        String message = available ? "Email is available" : "Email is already taken";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    /**
     * Get current authenticated user's info
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        UUID currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));
        logger.debug("Getting current user info: {}", currentUserId);

        UserDTO user = userService.getUserById(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", user));
    }

    /**
     * Update current user's profile
     */
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));
        logger.info("Updating my profile: {}", currentUserId);

        UserDTO updatedProfile = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
    }
}
