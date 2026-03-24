package com.usermanagement.service;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.repository.PermissionRepository;
import com.usermanagement.repository.RolePermissionRepository;
import com.usermanagement.service.dto.CreatePermissionRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.PermissionTreeDTO;
import com.usermanagement.service.dto.UpdatePermissionRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
 * Permission Service Implementation Test
 *
 * @author Test Team
 * @since 1.0
 */
class PermissionServiceImplTest {

    private PermissionServiceImpl permissionService;
    private PermissionRepository permissionRepository;
    private RolePermissionRepository rolePermissionRepository;
    private PermissionCacheService permissionCacheService;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionCacheService = mock(PermissionCacheService.class);

        permissionService = new PermissionServiceImpl(
                permissionRepository,
                rolePermissionRepository,
                permissionCacheService
        );
    }

    @Test
    void shouldCreatePermissionSuccessfully() {
        // Given
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Create User");
        request.setCode("user:create");
        request.setType(Permission.PermissionType.ACTION);
        request.setResource("user");
        request.setAction("create");
        request.setSortOrder(1);

        when(permissionRepository.existsByCode("user:create")).thenReturn(false);

        Permission savedPermission = createTestPermission(UUID.randomUUID(), "Create User", "user:create");
        when(permissionRepository.save(any(Permission.class))).thenReturn(savedPermission);

        // When
        PermissionDTO result = permissionService.createPermission(request);

        // Then
        assertNotNull(result);
        assertEquals("Create User", result.getName());
        assertEquals("user:create", result.getCode());

        ArgumentCaptor<Permission> permCaptor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(permCaptor.capture());
        Permission captured = permCaptor.getValue();
        assertEquals(Permission.PermissionStatus.ACTIVE, captured.getStatus());

        verify(permissionCacheService).evictAllPermissions();
    }

    @Test
    void shouldCreatePermissionWithParent() {
        // Given
        UUID parentId = UUID.randomUUID();
        Permission parent = createTestPermission(parentId, "User Menu", "user:menu");
        parent.setType(Permission.PermissionType.MENU);

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Create User");
        request.setCode("user:create");
        request.setType(Permission.PermissionType.ACTION);
        request.setResource("user");
        request.setParentId(parentId);

        when(permissionRepository.existsByCode("user:create")).thenReturn(false);
        when(permissionRepository.findById(parentId)).thenReturn(Optional.of(parent));

        Permission savedPermission = createTestPermission(UUID.randomUUID(), "Create User", "user:create");
        when(permissionRepository.save(any(Permission.class))).thenReturn(savedPermission);

        // When
        PermissionDTO result = permissionService.createPermission(request);

        // Then
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingPermissionWithDuplicateCode() {
        // Given
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setCode("user:existing");

        when(permissionRepository.existsByCode("user:existing")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> permissionService.createPermission(request));
        assertTrue(exception.getMessage().contains("Permission code already exists"));
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void shouldUpdatePermissionSuccessfully() {
        // Given
        UUID permissionId = UUID.randomUUID();
        Permission existingPermission = createTestPermission(permissionId, "Old Name", "user:create");

        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setName("Updated Name");
        request.setType(Permission.PermissionType.ACTION);
        request.setResource("user");
        request.setAction("create");

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(existingPermission));
        when(permissionRepository.save(any(Permission.class))).thenReturn(existingPermission);

        // When
        PermissionDTO result = permissionService.updatePermission(permissionId, request);

        // Then
        assertNotNull(result);
        verify(permissionRepository).save(any(Permission.class));
        verify(permissionCacheService).evictAllPermissions();
    }

    @Test
    void shouldThrowExceptionWhenCircularParentReference() {
        // Given
        UUID permissionId = UUID.randomUUID();
        Permission permission = createTestPermission(permissionId, "Test", "test:code");

        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setParentId(permissionId); // Try to set itself as parent

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> permissionService.updatePermission(permissionId, request));
        assertTrue(exception.getMessage().contains("cannot be its own parent"));
    }

    @Test
    void shouldDeletePermissionSuccessfully() {
        // Given
        UUID permissionId = UUID.randomUUID();
        Permission permission = createTestPermission(permissionId, "Test", "test:code");
        permission.setChildren(Collections.emptyList());

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.countByPermissionId(permissionId)).thenReturn(0L);
        when(permissionRepository.save(any(Permission.class))).thenReturn(permission);

        // When
        permissionService.deletePermission(permissionId);

        // Then
        ArgumentCaptor<Permission> permCaptor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(permCaptor.capture());
        assertNotNull(permCaptor.getValue().getDeletedAt());
        verify(permissionCacheService).evictAllPermissions();
    }

    @Test
    void shouldThrowExceptionWhenDeletingPermissionInUse() {
        // Given
        UUID permissionId = UUID.randomUUID();
        Permission permission = createTestPermission(permissionId, "Test", "test:code");

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.countByPermissionId(permissionId)).thenReturn(5L);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> permissionService.deletePermission(permissionId));
        assertTrue(exception.getMessage().contains("assigned to"));
    }

    @Test
    void shouldThrowExceptionWhenDeletingPermissionWithChildren() {
        // Given
        UUID permissionId = UUID.randomUUID();
        Permission permission = createTestPermission(permissionId, "Parent", "parent:code");

        Permission child = createTestPermission(UUID.randomUUID(), "Child", "child:code");
        permission.addChild(child);

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.countByPermissionId(permissionId)).thenReturn(0L);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> permissionService.deletePermission(permissionId));
        assertTrue(exception.getMessage().contains("has child permissions"));
    }

    @Test
    void shouldGetPermissionById() {
        // Given
        UUID permissionId = UUID.randomUUID();
        Permission permission = createTestPermission(permissionId, "Test", "test:code");

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

        // When
        PermissionDTO result = permissionService.getPermissionById(permissionId);

        // Then
        assertNotNull(result);
        assertEquals(permissionId, result.getId());
    }

    @Test
    void shouldThrowExceptionWhenPermissionNotFound() {
        // Given
        UUID permissionId = UUID.randomUUID();
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> permissionService.getPermissionById(permissionId));
        assertTrue(exception.getMessage().contains("Permission not found"));
    }

    @Test
    void shouldGetAllPermissions() {
        // Given
        List<Permission> permissions = Arrays.asList(
                createTestPermission(UUID.randomUUID(), "Perm 1", "code:1"),
                createTestPermission(UUID.randomUUID(), "Perm 2", "code:2")
        );

        when(permissionRepository.findAll()).thenReturn(permissions);

        // When
        List<PermissionDTO> result = permissionService.getAllPermissions();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldFilterOutDeletedPermissions() {
        // Given
        Permission activePerm = createTestPermission(UUID.randomUUID(), "Active", "active:code");
        Permission deletedPerm = createTestPermission(UUID.randomUUID(), "Deleted", "deleted:code");
        deletedPerm.setDeletedAt(Instant.now());

        when(permissionRepository.findAll()).thenReturn(Arrays.asList(activePerm, deletedPerm));

        // When
        List<PermissionDTO> result = permissionService.getAllPermissions();

        // Then
        assertEquals(1, result.size());
        assertEquals("Active", result.get(0).getName());
    }

    @Test
    void shouldGetAllActivePermissions() {
        // Given
        Permission activePerm = createTestPermission(UUID.randomUUID(), "Active", "active:code");

        when(permissionRepository.findByStatus(Permission.PermissionStatus.ACTIVE))
                .thenReturn(Collections.singletonList(activePerm));

        // When
        List<PermissionDTO> result = permissionService.getAllActivePermissions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetPermissionsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Permission> permissions = Collections.singletonList(
                createTestPermission(UUID.randomUUID(), "Test", "test:code")
        );
        Page<Permission> permissionPage = new PageImpl<>(permissions);

        when(permissionRepository.findByDeletedAtIsNull(pageable)).thenReturn(permissionPage);

        // When
        Page<PermissionDTO> result = permissionService.getPermissions(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldGetPermissionTree() {
        // Given
        Permission parent = createTestPermission(UUID.randomUUID(), "Parent", "parent:code");
        parent.setType(Permission.PermissionType.MENU);
        parent.setSortOrder(1);

        Permission child = createTestPermission(UUID.randomUUID(), "Child", "child:code");
        child.setParent(parent);
        child.setSortOrder(1);

        when(permissionRepository.findByStatus(Permission.PermissionStatus.ACTIVE))
                .thenReturn(Arrays.asList(parent, child));

        // When
        List<PermissionTreeDTO> result = permissionService.getPermissionTree();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Only root node
        assertEquals("Parent", result.get(0).getName());
    }

    @Test
    void shouldGetPermissionsByType() {
        // Given
        Permission menuPerm = createTestPermission(UUID.randomUUID(), "Menu", "menu:code");
        menuPerm.setType(Permission.PermissionType.MENU);

        when(permissionRepository.findByTypeAndStatus(Permission.PermissionType.MENU, Permission.PermissionStatus.ACTIVE))
                .thenReturn(Collections.singletonList(menuPerm));

        // When
        List<PermissionDTO> result = permissionService.getPermissionsByType(Permission.PermissionType.MENU);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetPermissionsByRoleId() {
        // Given
        UUID roleId = UUID.randomUUID();
        Permission permission = createTestPermission(UUID.randomUUID(), "Test", "test:code");

        when(permissionRepository.findByRoleId(roleId)).thenReturn(Collections.singletonList(permission));

        // When
        List<PermissionDTO> result = permissionService.getPermissionsByRoleId(roleId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetMenuPermissions() {
        // Given
        Permission menuPerm = createTestPermission(UUID.randomUUID(), "User Menu", "user:menu");
        menuPerm.setType(Permission.PermissionType.MENU);

        when(permissionRepository.findMenuPermissions()).thenReturn(Collections.singletonList(menuPerm));

        // When
        List<PermissionDTO> result = permissionService.getMenuPermissions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldFindByCode() {
        // Given
        Permission permission = createTestPermission(UUID.randomUUID(), "Test", "test:code");

        when(permissionRepository.findByCode("test:code")).thenReturn(Optional.of(permission));

        // When
        Optional<PermissionDTO> result = permissionService.findByCode("test:code");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test:code", result.get().getCode());
    }

    @Test
    void shouldCheckExistsByCode() {
        // Given
        when(permissionRepository.existsByCode("existing:code")).thenReturn(true);

        // When
        boolean result = permissionService.existsByCode("existing:code");

        // Then
        assertTrue(result);
    }

    @Test
    void shouldGetPermissionsByResource() {
        // Given
        Permission permission = createTestPermission(UUID.randomUUID(), "Create", "user:create");
        permission.setResource("user");

        when(permissionRepository.findByResource("user"))
                .thenReturn(Collections.singletonList(permission));

        // When
        List<PermissionDTO> result = permissionService.getPermissionsByResource("user");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldFindByResourceAndAction() {
        // Given
        Permission permission = createTestPermission(UUID.randomUUID(), "Create User", "user:create");
        permission.setResource("user");
        permission.setAction("create");

        when(permissionRepository.findByResourceAndAction("user", "create"))
                .thenReturn(Optional.of(permission));

        // When
        Optional<PermissionDTO> result = permissionService.findByResourceAndAction("user", "create");

        // Then
        assertTrue(result.isPresent());
        assertEquals("user:create", result.get().getCode());
    }

    @Test
    void shouldInitializeDefaultPermissions() {
        // Given
        when(permissionRepository.existsByCode(anyString())).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        permissionService.initializeDefaultPermissions();

        // Then
        verify(permissionRepository, atLeast(10)).save(any(Permission.class));
    }

    @Test
    void shouldSkipExistingPermissionsOnInit() {
        // Given
        when(permissionRepository.existsByCode(anyString())).thenReturn(true);

        // When
        permissionService.initializeDefaultPermissions();

        // Then
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    // Helper method
    private Permission createTestPermission(UUID id, String name, String code) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setName(name);
        permission.setCode(code);
        permission.setType(Permission.PermissionType.ACTION);
        permission.setResource("test");
        permission.setStatus(Permission.PermissionStatus.ACTIVE);
        permission.setSortOrder(0);
        permission.setCreatedAt(Instant.now());
        permission.setUpdatedAt(Instant.now());
        return permission;
    }
}
