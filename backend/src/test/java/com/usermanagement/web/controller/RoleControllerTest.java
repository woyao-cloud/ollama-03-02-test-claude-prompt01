package com.usermanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.Permission;
import com.usermanagement.security.SecurityConfig;
import com.usermanagement.service.RoleService;
import com.usermanagement.service.dto.*;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.AssignPermissionsRequest;
import com.usermanagement.web.dto.PageResponse;
import com.usermanagement.web.dto.UpdateDataScopeRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Role Controller Test
 *
 * @author Test Team
 * @since 1.0
 */
@WebMvcTest(RoleController.class)
@Import(SecurityConfig.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleService roleService;

    private UUID testRoleId;
    private RoleDTO testRoleDTO;

    @BeforeEach
    void setUp() {
        testRoleId = UUID.randomUUID();
        testRoleDTO = createTestRoleDTO(testRoleId, "Test Role", "ROLE_TEST");
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRolesWithPagination() throws Exception {
        // Given
        List<RoleDTO> roles = Arrays.asList(
                testRoleDTO,
                createTestRoleDTO(UUID.randomUUID(), "Role 2", "ROLE_2")
        );
        Page<RoleDTO> rolePage = new PageImpl<>(roles);

        when(roleService.getRoles(Mockito.any(RoleQueryRequest.class))).thenReturn(rolePage);

        // When & Then
        mockMvc.perform(get("/api/v1/roles")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRolesWithFilters() throws Exception {
        // Given
        Page<RoleDTO> rolePage = new PageImpl<>(Collections.singletonList(testRoleDTO));

        when(roleService.getRoles(Mockito.any(RoleQueryRequest.class))).thenReturn(rolePage);

        // When & Then
        mockMvc.perform(get("/api/v1/roles")
                        .param("keyword", "Test")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetAllActiveRoles() throws Exception {
        // Given
        List<RoleDTO> roles = Arrays.asList(
                testRoleDTO,
                createTestRoleDTO(UUID.randomUUID(), "Role 2", "ROLE_2")
        );

        when(roleService.getAllActiveRoles()).thenReturn(roles);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRoleById() throws Exception {
        // Given
        when(roleService.getRoleByIdWithPermissions(testRoleId)).thenReturn(testRoleDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/{id}", testRoleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testRoleId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRoleByCode() throws Exception {
        // Given
        when(roleService.getRoleByCode("ROLE_TEST")).thenReturn(testRoleDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/code/{code}", "ROLE_TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("ROLE_TEST"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldCreateRole() throws Exception {
        // Given
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("New Role");
        request.setCode("ROLE_NEW");
        request.setDescription("New role description");
        request.setDataScope(Role.DataScope.ALL);

        RoleDTO createdRole = createTestRoleDTO(UUID.randomUUID(), "New Role", "ROLE_NEW");

        when(roleService.createRole(Mockito.any(CreateRoleRequest.class))).thenReturn(createdRole);

        // When & Then
        mockMvc.perform(post("/api/v1/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Role"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldUpdateRole() throws Exception {
        // Given
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated Role");
        request.setDescription("Updated description");

        RoleDTO updatedRole = createTestRoleDTO(testRoleId, "Updated Role", "ROLE_TEST");

        when(roleService.updateRole(eq(testRoleId), Mockito.any(UpdateRoleRequest.class))).thenReturn(updatedRole);

        // When & Then
        mockMvc.perform(put("/api/v1/roles/{id}", testRoleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Role"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldDeleteRole() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/roles/{id}", testRoleId)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));

        verify(roleService).deleteRole(testRoleId);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldAssignPermissions() throws Exception {
        // Given
        UUID permissionId1 = UUID.randomUUID();
        UUID permissionId2 = UUID.randomUUID();

        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(Arrays.asList(permissionId1, permissionId2));

        // When & Then
        mockMvc.perform(post("/api/v1/roles/{id}/permissions", testRoleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(roleService).assignPermissions(testRoleId, Arrays.asList(permissionId1, permissionId2));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRolePermissions() throws Exception {
        // Given
        List<PermissionDTO> permissions = Arrays.asList(
                createTestPermissionDTO(UUID.randomUUID(), "user:create"),
                createTestPermissionDTO(UUID.randomUUID(), "user:read")
        );

        when(roleService.getRolePermissions(testRoleId)).thenReturn(permissions);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/{id}/permissions", testRoleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRolePermissionCodes() throws Exception {
        // Given
        List<String> permissionCodes = Arrays.asList("user:create", "user:read");

        when(roleService.getRolePermissionCodes(testRoleId)).thenReturn(permissionCodes);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/{id}/permission-codes", testRoleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("user:create"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldUpdateDataScope() throws Exception {
        // Given
        UpdateDataScopeRequest request = new UpdateDataScopeRequest();
        request.setDataScope(Role.DataScope.DEPT);

        RoleDTO updatedRole = createTestRoleDTO(testRoleId, "Test Role", "ROLE_TEST");
        updatedRole.setDataScope(Role.DataScope.DEPT);

        when(roleService.updateDataScope(testRoleId, Role.DataScope.DEPT)).thenReturn(updatedRole);

        // When & Then
        mockMvc.perform(patch("/api/v1/roles/{id}/data-scope", testRoleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dataScope").value("DEPT"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldCheckCodeExists() throws Exception {
        // Given
        when(roleService.existsByCode("ROLE_EXISTING")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/check-code")
                        .param("code", "ROLE_EXISTING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldGetRolesByUserId() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        List<RoleDTO> roles = Collections.singletonList(testRoleDTO);

        when(roleService.getRolesByUserId(userId)).thenReturn(roles);

        // When & Then
        mockMvc.perform(get("/api/v1/roles/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_READ")
    void shouldDenyAccessWhenCreatingRoleWithoutProperAuthority() throws Exception {
        // Given
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("New Role");
        request.setCode("ROLE_NEW");

        // When & Then
        mockMvc.perform(post("/api/v1/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldHandleServiceException() throws Exception {
        // Given
        when(roleService.getRoleByIdWithPermissions(Mockito.any())).thenThrow(new IllegalArgumentException("Role not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/roles/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    // Helper methods
    private RoleDTO createTestRoleDTO(UUID id, String name, String code) {
        RoleDTO dto = new RoleDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCode(code);
        dto.setDescription("Test description");
        dto.setDataScope(Role.DataScope.ALL);
        dto.setStatus(Role.RoleStatus.ACTIVE);
        dto.setCreatedAt(Instant.now());
        return dto;
    }

    private PermissionDTO createTestPermissionDTO(UUID id, String code) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setName(code.replace(":", " "));
        dto.setType(Permission.PermissionType.ACTION);
        return dto;
    }
}
