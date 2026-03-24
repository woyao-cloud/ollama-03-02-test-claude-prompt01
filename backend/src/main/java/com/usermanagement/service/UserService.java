package com.usermanagement.service;

import com.usermanagement.service.dto.CreateUserRequest;
import com.usermanagement.service.dto.UpdateProfileRequest;
import com.usermanagement.service.dto.UpdateUserRequest;
import com.usermanagement.service.dto.UserDTO;
import com.usermanagement.service.dto.UserQueryRequest;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * User Service Interface
 * Handles user management operations including CRUD, status management, and role assignment
 *
 * @author Service Team
 * @since 1.0
 */
public interface UserService {

    /**
     * Create a new user
     *
     * @param request user creation request
     * @return created user DTO
     */
    UserDTO createUser(CreateUserRequest request);

    /**
     * Update an existing user
     *
     * @param id user ID
     * @param request user update request
     * @return updated user DTO
     */
    UserDTO updateUser(UUID id, UpdateUserRequest request);

    /**
     * Delete a user (soft delete)
     *
     * @param id user ID
     */
    void deleteUser(UUID id);

    /**
     * Get user by ID
     *
     * @param id user ID
     * @return user DTO
     */
    UserDTO getUserById(UUID id);

    /**
     * Get user by email
     *
     * @param email user email
     * @return user DTO
     */
    UserDTO getUserByEmail(String email);

    /**
     * Get paginated list of users with optional filters
     *
     * @param query query request with filters and pagination
     * @return page of user DTOs
     */
    Page<UserDTO> getUsers(UserQueryRequest query);

    /**
     * Update user status
     *
     * @param id user ID
     * @param status new status
     */
    void updateStatus(UUID id, String status);

    /**
     * Assign roles to user (replace existing roles)
     *
     * @param userId user ID
     * @param roleIds list of role IDs
     */
    void assignRoles(UUID userId, List<UUID> roleIds);

    /**
     * Get user profile
     *
     * @param id user ID
     * @return user profile DTO
     */
    UserDTO getProfile(UUID id);

    /**
     * Update user profile
     *
     * @param id user ID
     * @param request profile update request
     * @return updated user DTO
     */
    UserDTO updateProfile(UUID id, UpdateProfileRequest request);

    /**
     * Check if email is available
     *
     * @param email email to check
     * @return true if available
     */
    boolean isEmailAvailable(String email);

    /**
     * Unlock a locked user account
     *
     * @param id user ID
     */
    void unlockUser(UUID id);
}
