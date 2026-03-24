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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Role Service Implementation Test
 *
 * @author Test Team
 * @since 1.0
 */
class RoleServiceImplTest {

    private RoleServiceImpl roleService;
    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private RolePermissionRepository rolePermissionRepository;
    private UserRoleRepository userRoleRepository;
    private PermissionCacheService permissionCacheService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        permissionCacheService = mock(PermissionCacheService.class);

        roleService = new RoleServiceImpl(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRoleRepository,
                permissionCacheService
        );
    }

    @Test
    void shouldCreateRoleSuccessfully() {
        // Given
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Test Role");
        request.setCode("ROLE_TEST");
        request.setDescription("Test role description");
        request.setDataScope(Role.DataScope.ALL);

        when(roleRepository.existsByCode("ROLE_TEST")).thenReturn(false);
        when(roleRepository.existsByName("Test Role")).thenReturn(false);

        Role savedRole = createTestRole(UUID.randomUUID(), "Test Role", "ROLE_TEST");
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        // When
        RoleDTO result = roleService.createRole(request);

        // Then
        assertNotNull(result);
        assertEquals("Test Role", result.getName());
        assertEquals("ROLE_TEST", result.getCode());

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        Role capturedRole = roleCaptor.getValue();
        assertEquals(Role.DataScope.ALL, capturedRole.getDataScope());
        assertEquals(Role.RoleStatus.ACTIVE, capturedRole.getStatus());
        assertFalse(capturedRole.getIsSystem());
    }

    @Test
    void shouldThrowExceptionWhenCreatingRoleWithDuplicateCode() {
        // Given
        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode("ROLE_EXISTING");

        when(roleRepository.existsByCode("ROLE_EXISTING")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.createRole(request));
        assertTrue(exception.getMessage().contains("Role code already exists"));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCreatingRoleWithDuplicateName() {
        // Given
        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode("ROLE_NEW");
        request.setName("Existing Name");

        when(roleRepository.existsByCode("ROLE_NEW")).thenReturn(false);
        when(roleRepository.existsByName("Existing Name")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.createRole(request));
        assertTrue(exception.getMessage().contains("Role name already exists"));
    }

    @Test
    void shouldCreateRoleWithPermissions() {
        // Given
        UUID permissionId = UUID.randomUUID();
        List<String> permissionCodes = Collections.singletonList("user:create");

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Test Role");
        request.setCode("ROLE_TEST");
        request.setPermissionCodes(permissionCodes);

        Permission permission = new Permission();
        permission.setId(permissionId);
        permission.setCode("user:create");

        Role savedRole = createTestRole(UUID.randomUUID(), "Test Role", "ROLE_TEST");

        when(roleRepository.existsByCode("ROLE_TEST")).thenReturn(false);
        when(roleRepository.existsByName("Test Role")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);
        when(roleRepository.findById(savedRole.getId())).thenReturn(Optional.of(savedRole));
        when(permissionRepository.findByCode("user:create")).thenReturn(Optional.of(permission));

        // When
        RoleDTO result = roleService.createRole(request);

        // Then
        verify(rolePermissionRepository).deleteByRoleId(savedRole.getId());
    }

    @Test
    void shouldUpdateRoleSuccessfully() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role existingRole = createTestRole(roleId, "Old Name", "ROLE_TEST");
        existingRole.setIsSystem(false);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated Name");
        request.setDescription("Updated description");
        request.setDataScope(Role.DataScope.DEPT);
        request.setStatus(Role.RoleStatus.ACTIVE);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.existsByName("Updated Name")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(existingRole);

        // When
        RoleDTO result = roleService.updateRole(roleId, request);

        // Then
        assertNotNull(result);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingSystemRole() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role systemRole = createTestRole(roleId, "System Role", "ROLE_SYSTEM");
        systemRole.setIsSystem(true);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated Name");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> roleService.updateRole(roleId, request));
        assertTrue(exception.getMessage().contains("Cannot modify system role"));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentRole() {
        // Given
        UUID roleId = UUID.randomUUID();
        UpdateRoleRequest request = new UpdateRoleRequest();

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.updateRole(roleId, request));
        assertTrue(exception.getMessage().contains("Role not found"));
    }

    @Test
    void shouldDeleteRoleSuccessfully() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");
        role.setIsSystem(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRoleRepository.findByRoleId(roleId)).thenReturn(Collections.emptyList());
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        // When
        roleService.deleteRole(roleId);

        // Then
        verify(roleRepository).save(any(Role.class));
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertNotNull(roleCaptor.getValue().getDeletedAt());
    }

    @Test
    void shouldThrowExceptionWhenDeletingSystemRole() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role systemRole = createTestRole(roleId, "System Role", "ROLE_SYSTEM");
        systemRole.setIsSystem(true);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> roleService.deleteRole(roleId));
        assertTrue(exception.getMessage().contains("Cannot delete system role"));
    }

    @Test
    void shouldThrowExceptionWhenDeletingRoleInUse() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");
        role.setIsSystem(false);

        UserRole userRole = new UserRole();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRoleRepository.findByRoleId(roleId)).thenReturn(Collections.singletonList(userRole));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> roleService.deleteRole(roleId));
        assertTrue(exception.getMessage().contains("assigned to"));
    }

    @Test
    void shouldGetRoleById() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        // When
        RoleDTO result = roleService.getRoleById(roleId);

        // Then
        assertNotNull(result);
        assertEquals(roleId, result.getId());
        assertEquals("Test Role", result.getName());
    }

    @Test
    void shouldGetRoleByIdWithPermissions() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");

        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("user:create");
        permission.setName("Create User");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByRoleId(roleId)).thenReturn(Collections.singletonList(permission));

        // When
        RoleDTO result = roleService.getRoleByIdWithPermissions(roleId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPermissions());
        assertEquals(1, result.getPermissions().size());
    }

    @Test
    void shouldGetAllRoles() {
        // Given
        List<Role> roles = Arrays.asList(
                createTestRole(UUID.randomUUID(), "Role 1", "ROLE_1"),
                createTestRole(UUID.randomUUID(), "Role 2", "ROLE_2")
        );

        when(roleRepository.findAllActive()).thenReturn(roles);

        // When
        List<RoleDTO> result = roleService.getAllRoles();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldGetAllActiveRoles() {
        // Given
        List<Role> roles = Arrays.asList(
                createTestRole(UUID.randomUUID(), "Role 1", "ROLE_1"),
                createTestRole(UUID.randomUUID(), "Role 2", "ROLE_2")
        );

        when(roleRepository.findByStatus(Role.RoleStatus.ACTIVE)).thenReturn(roles);

        // When
        List<RoleDTO> result = roleService.getAllActiveRoles();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldGetRolesWithPagination() {
        // Given
        RoleQueryRequest query = new RoleQueryRequest();
        query.setPage(0);
        query.setSize(10);
        query.setSortBy("createdAt");
        query.setSortDirection("DESC");

        List<Role> roles = Collections.singletonList(
                createTestRole(UUID.randomUUID(), "Test Role", "ROLE_TEST")
        );
        Page<Role> rolePage = new PageImpl<>(roles);

        when(roleRepository.findAllActive(any(Pageable.class))).thenReturn(rolePage);

        // When
        Page<RoleDTO> result = roleService.getRoles(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldAssignPermissions() {
        // Given
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        List<UUID> permissionIds = Collections.singletonList(permissionId);

        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");
        Permission permission = new Permission();
        permission.setId(permissionId);
        permission.setCode("user:create");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

        // When
        roleService.assignPermissions(roleId, permissionIds);

        // Then
        verify(rolePermissionRepository).deleteByRoleId(roleId);
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void shouldAssignPermissionsByCodes() {
        // Given
        UUID roleId = UUID.randomUUID();
        List<String> permissionCodes = Arrays.asList("user:create", "user:read");

        Permission permission1 = new Permission();
        permission1.setId(UUID.randomUUID());
        permission1.setCode("user:create");

        Permission permission2 = new Permission();
        permission2.setId(UUID.randomUUID());
        permission2.setCode("user:read");

        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode("user:create")).thenReturn(Optional.of(permission1));
        when(permissionRepository.findByCode("user:read")).thenReturn(Optional.of(permission2));

        // When
        roleService.assignPermissionsByCodes(roleId, permissionCodes);

        // Then
        verify(rolePermissionRepository).deleteByRoleId(roleId);
    }

    @Test
    void shouldGetRolePermissions() {
        // Given
        UUID roleId = UUID.randomUUID();
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("user:create");
        permission.setName("Create User");

        when(permissionRepository.findByRoleId(roleId)).thenReturn(Collections.singletonList(permission));

        // When
        List<PermissionDTO> result = roleService.getRolePermissions(roleId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user:create", result.get(0).getCode());
    }

    @Test
    void shouldGetRolePermissionCodes() {
        // Given
        UUID roleId = UUID.randomUUID();
        Permission permission = new Permission();
        permission.setCode("user:create");

        when(permissionRepository.findByRoleId(roleId)).thenReturn(Collections.singletonList(permission));

        // When
        List<String> result = roleService.getRolePermissionCodes(roleId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user:create", result.get(0));
    }

    @Test
    void shouldUpdateDataScope() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role role = createTestRole(roleId, "Test Role", "ROLE_TEST");
        role.setDataScope(Role.DataScope.ALL);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        // When
        RoleDTO result = roleService.updateDataScope(roleId, Role.DataScope.DEPT);

        // Then
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void shouldCheckExistsByCode() {
        // Given
        when(roleRepository.existsByCode("ROLE_TEST")).thenReturn(true);

        // When
        boolean result = roleService.existsByCode("ROLE_TEST");

        // Then
        assertTrue(result);
    }

    @Test
    void shouldGetRolesByUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        List<Role> roles = Collections.singletonList(
                createTestRole(UUID.randomUUID(), "Test Role", "ROLE_TEST")
        );

        when(roleRepository.findByUserId(userId)).thenReturn(roles);

        // When
        List<RoleDTO> result = roleService.getRolesByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetRoleCodesByUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        Role role = createTestRole(UUID.randomUUID(), "Test Role", "ROLE_TEST");

        when(roleRepository.findByUserId(userId)).thenReturn(Collections.singletonList(role));

        // When
        List<String> result = roleService.getRoleCodesByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ROLE_TEST", result.get(0));
    }

    @Test
    void shouldGetRoleByCode() {
        // Given
        Role role = createTestRole(UUID.randomUUID(), "Test Role", "ROLE_TEST");

        when(roleRepository.findByCode("ROLE_TEST")).thenReturn(Optional.of(role));

        // When
        RoleDTO result = roleService.getRoleByCode("ROLE_TEST");

        // Then
        assertNotNull(result);
        assertEquals("ROLE_TEST", result.getCode());
    }

    @Test
    void shouldThrowExceptionWhenRoleNotFoundByCode() {
        // Given
        when(roleRepository.findByCode("ROLE_NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.getRoleByCode("ROLE_NONEXISTENT"));
        assertTrue(exception.getMessage().contains("Role not found"));
    }

    // Helper method
    private Role createTestRole(UUID id, String name, String code) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setCode(code);
        role.setDescription("Test description");
        role.setDataScope(Role.DataScope.ALL);
        role.setStatus(Role.RoleStatus.ACTIVE);
        role.setIsSystem(false);
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());
        return role;
    }
}
