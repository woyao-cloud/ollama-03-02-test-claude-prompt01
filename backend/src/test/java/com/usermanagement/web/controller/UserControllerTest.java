package com.usermanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.User;
import com.usermanagement.security.SecurityConfig;
import com.usermanagement.security.SecurityUtils;
import com.usermanagement.service.UserService;
import com.usermanagement.service.dto.*;
import com.usermanagement.web.dto.AssignRolesRequest;
import com.usermanagement.web.dto.UpdateUserStatusRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User Controller Test
 *
 * @author Test Team
 * @since 1.0
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private UUID testUserId;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserDTO = createTestUserDTO(testUserId, "test@example.com", "John", "Doe");
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldGetUsersWithPagination() throws Exception {
        // Given
        List<UserDTO> users = Arrays.asList(
                testUserDTO,
                createTestUserDTO(UUID.randomUUID(), "user2@example.com", "Jane", "Smith")
        );
        Page<UserDTO> userPage = new PageImpl<>(users);

        when(userService.getUsers(any(UserQueryRequest.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldGetUsersWithFilters() throws Exception {
        // Given
        Page<UserDTO> userPage = new PageImpl<>(Collections.singletonList(testUserDTO));
        when(userService.getUsers(any(UserQueryRequest.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("email", "test@example.com")
                        .param("firstName", "John")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldGetUserById() throws Exception {
        // Given
        when(userService.getUserById(testUserId)).thenReturn(testUserDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldCreateUser() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPassword("password123");

        UserDTO createdUser = createTestUserDTO(UUID.randomUUID(), "new@example.com", "New", "User");

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(createdUser);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new@example.com"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnBadRequestWhenCreatingUserWithInvalidData() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldUpdateUser() throws Exception {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");

        UserDTO updatedUser = createTestUserDTO(testUserId, "test@example.com", "Updated", "Name");

        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", testUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Updated"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldUpdateUserStatus() throws Exception {
        // Given
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus("ACTIVE");

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{id}/status", testUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).updateStatus(testUserId, "ACTIVE");
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldDeleteUser() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", testUserId)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).deleteUser(testUserId);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldAssignRoles() throws Exception {
        // Given
        UUID roleId1 = UUID.randomUUID();
        UUID roleId2 = UUID.randomUUID();

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(Arrays.asList(roleId1, roleId2));

        // When & Then
        mockMvc.perform(post("/api/v1/users/{id}/roles", testUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).assignRoles(testUserId, Arrays.asList(roleId1, roleId2));
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldGetProfile() throws Exception {
        // Given
        when(userService.getProfile(testUserId)).thenReturn(testUserDTO);
        when(securityUtils.isCurrentUser(testUserId)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}/profile", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()));
    }

    @Test
    @WithMockUser(authorities = "USER_PROFILE_UPDATE")
    void shouldUpdateProfile() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");

        UserDTO updatedUser = createTestUserDTO(testUserId, "test@example.com", "Updated", "Name");

        when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class))).thenReturn(updatedUser);
        when(securityUtils.isCurrentUser(testUserId)).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}/profile", testUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Updated"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldUnlockUser() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users/{id}/unlock", testUserId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).unlockUser(testUserId);
    }

    @Test
    void shouldCheckEmailAvailability() throws Exception {
        // Given
        when(userService.isEmailAvailable("available@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/users/check-email")
                        .param("email", "available@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void shouldReturnFalseWhenEmailIsTaken() throws Exception {
        // Given
        when(userService.isEmailAvailable("taken@example.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/users/check-email")
                        .param("email", "taken@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @WithMockUser(username = "test-user")
    void shouldGetCurrentUser() throws Exception {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userService.getUserById(currentUserId)).thenReturn(testUserDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()));
    }

    @Test
    @WithMockUser(username = "test-user")
    void shouldUpdateMyProfile() throws Exception {
        // Given
        UUID currentUserId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Updated");

        UserDTO updatedUser = createTestUserDTO(testUserId, "test@example.com", "Updated", "Doe");

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userService.updateProfile(eq(currentUserId), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Updated"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldDenyAccessWhenCreatingUserWithoutProperAuthority() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldHandleServiceException() throws Exception {
        // Given
        when(userService.getUserById(any())).thenThrow(new IllegalArgumentException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldGetUsersWithRoleFilter() throws Exception {
        // Given
        UUID roleId = UUID.randomUUID();
        Page<UserDTO> userPage = new PageImpl<>(Collections.singletonList(testUserDTO));

        when(userService.getUsers(any(UserQueryRequest.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("roleId", roleId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldGetUsersWithDepartmentFilter() throws Exception {
        // Given
        UUID departmentId = UUID.randomUUID();
        Page<UserDTO> userPage = new PageImpl<>(Collections.singletonList(testUserDTO));

        when(userService.getUsers(any(UserQueryRequest.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // Helper method
    private UserDTO createTestUserDTO(UUID id, String email, String firstName, String lastName) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setFullName(firstName + " " + lastName);
        dto.setStatus(User.UserStatus.ACTIVE);
        dto.setEmailVerified(true);
        dto.setCreatedAt(Instant.now());
        dto.setUpdatedAt(Instant.now());
        return dto;
    }
}
