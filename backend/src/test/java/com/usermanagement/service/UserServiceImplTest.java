package com.usermanagement.service;

import com.usermanagement.domain.entity.Department;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.User;
import com.usermanagement.domain.entity.UserRole;
import com.usermanagement.repository.DepartmentRepository;
import com.usermanagement.repository.RoleRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.repository.UserRoleRepository;
import com.usermanagement.service.dto.CreateUserRequest;
import com.usermanagement.service.dto.UpdateProfileRequest;
import com.usermanagement.service.dto.UpdateUserRequest;
import com.usermanagement.service.dto.UserDTO;
import com.usermanagement.service.dto.UserQueryRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * User Service Implementation Test
 *
 * @author Test Team
 * @since 1.0
 */
class UserServiceImplTest {

    private UserServiceImpl userService;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private DepartmentRepository departmentRepository;
    private UserRoleRepository userRoleRepository;
    private SessionService sessionService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        sessionService = mock(SessionService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        userService = new UserServiceImpl(
                userRepository,
                roleRepository,
                departmentRepository,
                userRoleRepository,
                sessionService,
                passwordEncoder
        );
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("password123");
        request.setPhone("1234567890");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = createTestUser(UUID.randomUUID(), "test@example.com", "John", "Doe");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserDTO result = userService.createUser(request);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("encodedPassword", capturedUser.getPasswordHash());
        assertEquals(User.UserStatus.PENDING, capturedUser.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenCreatingUserWithDuplicateEmail() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));
        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCreateUserWithDepartment() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = new Department();
        department.setId(departmentId);
        department.setName("IT Department");

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setDepartmentId(departmentId);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        User savedUser = createTestUser(UUID.randomUUID(), "test@example.com", "John", "Doe");
        savedUser.setDepartment(department);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserDTO result = userService.createUser(request);

        // Then
        assertNotNull(result);
        assertEquals(departmentId, result.getDepartmentId());
        assertEquals("IT Department", result.getDepartmentName());
    }

    @Test
    void shouldCreateUserWithRoles() {
        // Given
        UUID roleId = UUID.randomUUID();
        List<UUID> roleIds = Collections.singletonList(roleId);

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setRoleIds(roleIds);

        Role role = new Role();
        role.setId(roleId);
        role.setName("ADMIN");
        role.setCode("ROLE_ADMIN");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        UUID userId = UUID.randomUUID();
        User savedUser = createTestUser(userId, "test@example.com", "John", "Doe");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(savedUser));

        // When
        UserDTO result = userService.createUser(request);

        // Then
        verify(userRoleRepository).deleteByUserId(userId);
        verify(sessionService).invalidateAllUserSessions(userId);
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        User existingUser = createTestUser(userId, "old@example.com", "Old", "Name");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setEmail("new@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        UserDTO result = userService.updateUser(userId, request);

        // Then
        assertNotNull(result);
        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingDeletedUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User deletedUser = createTestUser(userId, "test@example.com", "John", "Doe");
        deletedUser.softDelete();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New");

        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(userId, request));
        assertTrue(exception.getMessage().contains("Cannot update deleted user"));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(userId, request));
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void shouldSoftDeleteUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.deleteUser(userId);

        // Then
        verify(sessionService).invalidateAllUserSessions(userId);
        assertTrue(user.isDeleted());
    }

    @Test
    void shouldThrowExceptionWhenDeletingAlreadyDeletedUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.softDelete();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteUser(userId));
        assertTrue(exception.getMessage().contains("already deleted"));
    }

    @Test
    void shouldGetUserById() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserDTO result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenGettingDeletedUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.softDelete();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(userId));
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void shouldGetUserByEmail() {
        // Given
        String email = "test@example.com";
        User user = createTestUser(UUID.randomUUID(), email, "John", "Doe");

        when(userRepository.findActiveByEmail(email)).thenReturn(Optional.of(user));

        // When
        UserDTO result = userService.getUserByEmail(email);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGetUsersWithPagination() {
        // Given
        UserQueryRequest query = new UserQueryRequest();
        query.setPage(0);
        query.setSize(10);
        query.setSortBy("createdAt");
        query.setSortDirection("DESC");

        List<User> users = Arrays.asList(
                createTestUser(UUID.randomUUID(), "user1@example.com", "User", "One"),
                createTestUser(UUID.randomUUID(), "user2@example.com", "User", "Two")
        );
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getUsers(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldUpdateUserStatus() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setStatus(User.UserStatus.PENDING);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.updateStatus(userId, "ACTIVE");

        // Then
        assertEquals(User.UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldThrowExceptionForInvalidStatus() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateStatus(userId, "INVALID_STATUS"));
        assertTrue(exception.getMessage().contains("Invalid status"));
    }

    @Test
    void shouldUnlockUserWhenActivating() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setStatus(User.UserStatus.LOCKED);
        user.setLockedUntil(Instant.now().plusSeconds(3600));
        user.setFailedLoginAttempts(5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.updateStatus(userId, "ACTIVE");

        // Then
        assertEquals(User.UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getLockedUntil());
    }

    @Test
    void shouldAssignRolesToUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID roleId1 = UUID.randomUUID();
        UUID roleId2 = UUID.randomUUID();
        List<UUID> roleIds = Arrays.asList(roleId1, roleId2);

        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setUserRoles(new HashSet<>());

        Role role1 = new Role();
        role1.setId(roleId1);
        role1.setName("ADMIN");
        role1.setCode("ROLE_ADMIN");

        Role role2 = new Role();
        role2.setId(roleId2);
        role2.setName("USER");
        role2.setCode("ROLE_USER");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId1)).thenReturn(Optional.of(role1));
        when(roleRepository.findById(roleId2)).thenReturn(Optional.of(role2));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.assignRoles(userId, roleIds);

        // Then
        verify(userRoleRepository).deleteByUserId(userId);
        verify(sessionService).invalidateAllUserSessions(userId);
        assertEquals(2, user.getUserRoles().size());
    }

    @Test
    void shouldThrowExceptionWhenAssigningRolesToDeletedUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.softDelete();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.assignRoles(userId, Collections.singletonList(UUID.randomUUID())));
        assertTrue(exception.getMessage().contains("Cannot assign roles to deleted user"));
    }

    @Test
    void shouldThrowExceptionWhenAssigningDeletedRole() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setUserRoles(new HashSet<>());

        Role role = new Role();
        role.setId(roleId);
        role.setName("DELETED_ROLE");
        role.setCode("ROLE_DELETED");
        role.softDelete();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.assignRoles(userId, Collections.singletonList(roleId)));
        assertTrue(exception.getMessage().contains("Cannot assign deleted role"));
    }

    @Test
    void shouldGetProfile() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserDTO result = userService.getProfile(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void shouldUpdateProfile() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setPhone("9876543210");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDTO result = userService.updateProfile(userId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        assertEquals("Name", result.getLastName());
    }

    @Test
    void shouldUpdateProfileWithPasswordChange() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setPasswordHash("oldEncodedPassword");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "oldEncodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDTO result = userService.updateProfile(userId, request);

        // Then
        assertNotNull(result);
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    void shouldThrowExceptionWhenPasswordChangeWithoutCurrentPassword() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword(null);
        request.setNewPassword("newPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(userId, request));
        assertTrue(exception.getMessage().contains("Current password is required"));
    }

    @Test
    void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setPasswordHash("encodedPassword");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(userId, request));
        assertTrue(exception.getMessage().contains("Current password is incorrect"));
    }

    @Test
    void shouldReturnTrueWhenEmailIsAvailable() {
        // Given
        String email = "available@example.com";
        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(false);

        // When
        boolean result = userService.isEmailAvailable(email);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenEmailIsNotAvailable() {
        // Given
        String email = "taken@example.com";
        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(true);

        // When
        boolean result = userService.isEmailAvailable(email);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForEmptyEmail() {
        // When
        boolean result = userService.isEmailAvailable("");

        // Then
        assertFalse(result);
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void shouldReturnFalseForNullEmail() {
        // When
        boolean result = userService.isEmailAvailable(null);

        // Then
        assertFalse(result);
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void shouldUnlockUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.setStatus(User.UserStatus.LOCKED);
        user.setLockedUntil(Instant.now().plusSeconds(3600));
        user.setFailedLoginAttempts(5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.unlockUser(userId);

        // Then
        assertEquals(User.UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getLockedUntil());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    void shouldThrowExceptionWhenUnlockingDeletedUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "test@example.com", "John", "Doe");
        user.softDelete();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.unlockUser(userId));
        assertTrue(exception.getMessage().contains("Cannot unlock deleted user"));
    }

    // Helper method
    private User createTestUser(UUID id, String email, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}
