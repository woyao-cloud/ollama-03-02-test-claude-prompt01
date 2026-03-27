package com.usermanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.Permission;
import com.usermanagement.security.SecurityConfig;
import com.usermanagement.service.PermissionService;
import com.usermanagement.service.dto.CreatePermissionRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.PermissionTreeDTO;
import com.usermanagement.service.dto.UpdatePermissionRequest;

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
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Permission Controller Test
 *
 * @author Test Team
 * @since 1.0
 */
@WebMvcTest(PermissionController.class)
@Import(SecurityConfig.class)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PermissionService permissionService;

    private UUID testPermissionId;
    private PermissionDTO testPermissionDTO;

    @BeforeEach
    void setUp() {
        testPermissionId = UUID.randomUUID();
        testPermissionDTO = createTestPermissionDTO(testPermissionId, "user:create", "Create User");
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetPermissionsWithPagination() throws Exception {
        // Given
        List<PermissionDTO> permissions = Arrays.asList(
                testPermissionDTO,
                createTestPermissionDTO(UUID.randomUUID(), "user:read", "View User")
        );
        Page<PermissionDTO> permissionPage = new PageImpl<>(permissions);

        when(permissionService.getPermissions(any())).thenReturn(permissionPage);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetAllActivePermissions() throws Exception {
        // Given
        List<PermissionDTO> permissions = Arrays.asList(
                testPermissionDTO,
                createTestPermissionDTO(UUID.randomUUID(), "user:read", "View User")
        );

        when(permissionService.getAllActivePermissions()).thenReturn(permissions);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetPermissionTree() throws Exception {
        // Given
        PermissionTreeDTO root = new PermissionTreeDTO();
        root.setId(UUID.randomUUID());
        root.setName("User Menu");
        root.setCode("user:menu");
        root.setType(Permission.PermissionType.MENU);

        List<PermissionTreeDTO> tree = Collections.singletonList(root);

        when(permissionService.getPermissionTree()).thenReturn(tree);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("User Menu"));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetMenuPermissions() throws Exception {
        // Given
        List<PermissionDTO> menuPermissions = Arrays.asList(
                createTestPermissionDTO(UUID.randomUUID(), "user:menu", "User Menu"),
                createTestPermissionDTO(UUID.randomUUID(), "role:menu", "Role Menu")
        );

        when(permissionService.getMenuPermissions()).thenReturn(menuPermissions);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetPermissionsByType() throws Exception {
        // Given
        List<PermissionDTO> actionPermissions = Collections.singletonList(testPermissionDTO);

        when(permissionService.getPermissionsByType(Permission.PermissionType.ACTION))
                .thenReturn(actionPermissions);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/type/{type}", "ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetPermissionById() throws Exception {
        // Given
        when(permissionService.getPermissionById(testPermissionId)).thenReturn(testPermissionDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/{id}", testPermissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testPermissionId.toString()))
                .andExpect(jsonPath("$.data.code").value("user:create"));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetPermissionByCode() throws Exception {
        // Given
        when(permissionService.findByCode("user:create")).thenReturn(Optional.of(testPermissionDTO));

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/code/{code}", "user:create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("user:create"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldCreatePermission() throws Exception {
        // Given
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("New Permission");
        request.setCode("new:permission");
        request.setType(Permission.PermissionType.ACTION);
        request.setResource("new");
        request.setAction("permission");

        PermissionDTO createdPermission = createTestPermissionDTO(UUID.randomUUID(), "new:permission", "New Permission");

        when(permissionService.createPermission(Mockito.any(CreatePermissionRequest.class))).thenReturn(createdPermission);

        // When & Then
        mockMvc.perform(post("/api/v1/permissions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("new:permission"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldUpdatePermission() throws Exception {
        // Given
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setName("Updated Permission");
        request.setResource("updated");

        PermissionDTO updatedPermission = createTestPermissionDTO(testPermissionId, "user:create", "Updated Permission");

        when(permissionService.updatePermission(eq(testPermissionId), Mockito.any(UpdatePermissionRequest.class)))
                .thenReturn(updatedPermission);

        // When & Then
        mockMvc.perform(put("/api/v1/permissions/{id}", testPermissionId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Permission"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldDeletePermission() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/permissions/{id}", testPermissionId)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));

        verify(permissionService).deletePermission(testPermissionId);
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldGetPermissionsByResource() throws Exception {
        // Given
        List<PermissionDTO> permissions = Arrays.asList(
                createTestPermissionDTO(UUID.randomUUID(), "user:create", "Create User"),
                createTestPermissionDTO(UUID.randomUUID(), "user:read", "View User")
        );

        when(permissionService.getPermissionsByResource("user")).thenReturn(permissions);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/resource/{resource}", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldCheckCodeExists() throws Exception {
        // Given
        when(permissionService.existsByCode("existing:code")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/check-code")
                        .param("code", "existing:code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldInitializeDefaultPermissions() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/permissions/init-defaults")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(permissionService).initializeDefaultPermissions();
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_READ")
    void shouldDenyAccessWhenCreatingPermissionWithoutProperAuthority() throws Exception {
        // Given
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("New Permission");
        request.setCode("new:permission");

        // When & Then
        mockMvc.perform(post("/api/v1/permissions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldHandleServiceException() throws Exception {
        // Given
        when(permissionService.getPermissionById(any()))
                .thenThrow(new IllegalArgumentException("Permission not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldHandlePermissionNotFoundByCode() throws Exception {
        // Given
        when(permissionService.findByCode("nonexistent:code")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/permissions/code/{code}", "nonexistent:code"))
                .andExpect(status().isBadRequest());
    }

    // Helper method
    private PermissionDTO createTestPermissionDTO(UUID id, String code, String name) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setName(name);
        dto.setType(Permission.PermissionType.ACTION);
        dto.setResource(code.split(":")[0]);
        dto.setAction(code.split(":").length > 1 ? code.split(":")[1] : null);
        dto.setStatus(Permission.PermissionStatus.ACTIVE);
        dto.setCreatedAt(Instant.now());
        return dto;
    }
}
