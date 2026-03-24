package com.usermanagement.service;

import com.usermanagement.domain.entity.Department;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.DepartmentRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.dto.CreateDepartmentRequest;
import com.usermanagement.service.dto.DepartmentDTO;
import com.usermanagement.service.dto.DepartmentQueryRequest;
import com.usermanagement.service.dto.UpdateDepartmentRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Department Service Implementation Test
 *
 * @author Test Team
 * @since 1.0
 */
class DepartmentServiceImplTest {

    private DepartmentServiceImpl departmentService;
    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);

        departmentService = new DepartmentServiceImpl(
                departmentRepository,
                userRepository
        );
    }

    @Test
    void shouldCreateDepartmentSuccessfully() {
        // Given
        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("IT Department");
        request.setCode("IT");
        request.setDescription("Information Technology Department");
        request.setSortOrder(10);

        when(departmentRepository.existsByCode("IT")).thenReturn(false);

        UUID departmentId = UUID.randomUUID();
        Department savedDepartment = createTestDepartment(departmentId, "IT Department", "IT", null);
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);

        // When
        DepartmentDTO result = departmentService.createDepartment(request);

        // Then
        assertNotNull(result);
        assertEquals("IT Department", result.getName());
        assertEquals("IT", result.getCode());

        ArgumentCaptor<Department> deptCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository, times(2)).save(deptCaptor.capture());
    }

    @Test
    void shouldThrowExceptionWhenCreatingDepartmentWithDuplicateCode() {
        // Given
        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setCode("DUPLICATE");

        when(departmentRepository.existsByCode("DUPLICATE")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(request));
        assertTrue(exception.getMessage().contains("Department code already exists"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void shouldCreateDepartmentWithParent() {
        // Given
        UUID parentId = UUID.randomUUID();
        Department parent = createTestDepartment(parentId, "Headquarters", "HQ", null);
        parent.setLevel(1);
        parent.setPath("/" + parentId);

        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("IT Department");
        request.setCode("IT");
        request.setParentId(parentId);

        when(departmentRepository.existsByCode("IT")).thenReturn(false);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        UUID departmentId = UUID.randomUUID();
        Department savedDepartment = createTestDepartment(departmentId, "IT Department", "IT", parent);
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);

        // When
        DepartmentDTO result = departmentService.createDepartment(request);

        // Then
        assertNotNull(result);
        assertEquals(parentId, result.getParentId());
        assertEquals("Headquarters", result.getParentName());
    }

    @Test
    void shouldThrowExceptionWhenCreatingDepartmentWithDeletedParent() {
        // Given
        UUID parentId = UUID.randomUUID();
        Department parent = createTestDepartment(parentId, "Deleted Dept", "DELETED", null);
        parent.softDelete();

        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("IT Department");
        request.setCode("IT");
        request.setParentId(parentId);

        when(departmentRepository.existsByCode("IT")).thenReturn(false);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(request));
        assertTrue(exception.getMessage().contains("Cannot use deleted department as parent"));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDepartmentBeyondMaxLevel() {
        // Given
        UUID parentId = UUID.randomUUID();
        Department parent = createTestDepartment(parentId, "Level 5", "LVL5", null);
        parent.setLevel(5);

        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("Level 6");
        request.setCode("LVL6");
        request.setParentId(parentId);

        when(departmentRepository.existsByCode("LVL6")).thenReturn(false);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(request));
        assertTrue(exception.getMessage().contains("Cannot create department beyond level"));
    }

    @Test
    void shouldUpdateDepartmentSuccessfully() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department existingDept = createTestDepartment(departmentId, "Old Name", "OLD", null);

        UpdateDepartmentRequest request = new UpdateDepartmentRequest();
        request.setName("New Name");
        request.setCode("NEW");
        request.setDescription("Updated description");
        request.setSortOrder(20);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDept));
        when(departmentRepository.existsByCode("NEW")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDept);

        // When
        DepartmentDTO result = departmentService.updateDepartment(departmentId, request);

        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("NEW", result.getCode());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingDeletedDepartment() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department deletedDept = createTestDepartment(departmentId, "Deleted", "DELETED", null);
        deletedDept.softDelete();

        UpdateDepartmentRequest request = new UpdateDepartmentRequest();
        request.setName("New Name");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(deletedDept));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.updateDepartment(departmentId, request));
        assertTrue(exception.getMessage().contains("Cannot update deleted department"));
    }

    @Test
    void shouldDeleteDepartmentSuccessfully() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "To Delete", "DELETE", null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.countByParentId(departmentId)).thenReturn(0L);
        when(userRepository.countByDepartmentId(departmentId)).thenReturn(0L);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        // When
        departmentService.deleteDepartment(departmentId);

        // Then
        assertTrue(department.isDeleted());
    }

    @Test
    void shouldThrowExceptionWhenDeletingDepartmentWithChildren() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "Parent", "PARENT", null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.countByParentId(departmentId)).thenReturn(2L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.deleteDepartment(departmentId));
        assertTrue(exception.getMessage().contains("Cannot delete department with children"));
    }

    @Test
    void shouldThrowExceptionWhenDeletingDepartmentWithUsers() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "With Users", "USERS", null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.countByParentId(departmentId)).thenReturn(0L);
        when(userRepository.countByDepartmentId(departmentId)).thenReturn(5L);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.deleteDepartment(departmentId));
        assertTrue(exception.getMessage().contains("Cannot delete department with assigned users"));
    }

    @Test
    void shouldGetDepartmentById() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "IT", "IT", null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.countByDepartmentId(departmentId)).thenReturn(10L);

        // When
        DepartmentDTO result = departmentService.getDepartmentById(departmentId);

        // Then
        assertNotNull(result);
        assertEquals(departmentId, result.getId());
        assertEquals("IT", result.getName());
        assertEquals(10L, result.getUserCount());
    }

    @Test
    void shouldThrowExceptionWhenGettingDeletedDepartment() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "Deleted", "DELETED", null);
        department.softDelete();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.getDepartmentById(departmentId));
        assertTrue(exception.getMessage().contains("Department not found"));
    }

    @Test
    void shouldGetDepartmentByCode() {
        // Given
        String code = "IT";
        Department department = createTestDepartment(UUID.randomUUID(), "IT Department", code, null);

        when(departmentRepository.findByCode(code)).thenReturn(Optional.of(department));
        when(userRepository.countByDepartmentId(any())).thenReturn(0L);

        // When
        DepartmentDTO result = departmentService.getDepartmentByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    void shouldGetDepartmentsWithPagination() {
        // Given
        DepartmentQueryRequest query = new DepartmentQueryRequest();
        query.setPage(0);
        query.setSize(10);
        query.setSortBy("sortOrder");
        query.setSortDirection("DESC");

        List<Department> departments = Arrays.asList(
                createTestDepartment(UUID.randomUUID(), "Dept 1", "D1", null),
                createTestDepartment(UUID.randomUUID(), "Dept 2", "D2", null)
        );
        Page<Department> departmentPage = new PageImpl<>(departments);

        when(departmentRepository.findAllActive(any(Pageable.class))).thenReturn(departmentPage);
        when(userRepository.countByDepartmentId(any())).thenReturn(0L);

        // When
        Page<DepartmentDTO> result = departmentService.getDepartments(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldGetDepartmentTree() {
        // Given
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        Department root = createTestDepartment(rootId, "Root", "ROOT", null);
        root.setLevel(1);
        root.setPath("/" + rootId);

        Department child = createTestDepartment(childId, "Child", "CHILD", root);
        child.setLevel(2);
        child.setPath("/" + rootId + "/" + childId);

        List<Department> allDepartments = Arrays.asList(root, child);

        when(departmentRepository.findAllActive()).thenReturn(allDepartments);
        when(userRepository.countByDepartmentId(any())).thenReturn(0L);

        // When
        List<DepartmentDTO> result = departmentService.getDepartmentTree(true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Only root at top level
        assertEquals("Root", result.get(0).getName());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("Child", result.get(0).getChildren().get(0).getName());
    }

    @Test
    void shouldGetChildren() {
        // Given
        UUID parentId = UUID.randomUUID();
        Department parent = createTestDepartment(parentId, "Parent", "PARENT", null);

        List<Department> children = Arrays.asList(
                createTestDepartment(UUID.randomUUID(), "Child 1", "CHILD1", parent),
                createTestDepartment(UUID.randomUUID(), "Child 2", "CHILD2", parent)
        );

        when(departmentRepository.findByParentIdAndStatus(parentId, Department.DepartmentStatus.ACTIVE))
                .thenReturn(children);
        when(userRepository.countByDepartmentId(any())).thenReturn(0L);

        // When
        List<DepartmentDTO> result = departmentService.getChildren(parentId, false);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldUpdateDepartmentStatus() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "IT", "IT", null);
        department.setStatus(Department.DepartmentStatus.ACTIVE);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        // When
        departmentService.updateStatus(departmentId, "INACTIVE");

        // Then
        assertEquals(Department.DepartmentStatus.INACTIVE, department.getStatus());
    }

    @Test
    void shouldThrowExceptionForInvalidDepartmentStatus() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "IT", "IT", null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.updateStatus(departmentId, "INVALID_STATUS"));
        assertTrue(exception.getMessage().contains("Invalid status"));
    }

    @Test
    void shouldMoveDepartment() {
        // Given
        UUID departmentId = UUID.randomUUID();
        UUID newParentId = UUID.randomUUID();

        Department department = createTestDepartment(departmentId, "To Move", "MOVE", null);
        department.setLevel(1);
        department.setPath("/" + departmentId);

        Department newParent = createTestDepartment(newParentId, "New Parent", "PARENT", null);
        newParent.setLevel(1);
        newParent.setPath("/" + newParentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParent));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        // When
        departmentService.moveDepartment(departmentId, newParentId);

        // Then
        verify(departmentRepository).save(department);
    }

    @Test
    void shouldThrowExceptionWhenMovingToSelf() {
        // Given
        UUID departmentId = UUID.randomUUID();
        Department department = createTestDepartment(departmentId, "Self", "SELF", null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.moveDepartment(departmentId, departmentId));
        assertTrue(exception.getMessage().contains("Cannot be its own parent"));
    }

    @Test
    void shouldThrowExceptionWhenMovingToDescendant() {
        // Given
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        Department root = createTestDepartment(rootId, "Root", "ROOT", null);
        root.setLevel(1);
        root.setPath("/" + rootId);

        Department child = createTestDepartment(childId, "Child", "CHILD", root);
        child.setLevel(2);
        child.setPath("/" + rootId + "/" + childId);

        // Setup children lookup
        when(departmentRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(departmentRepository.findById(childId)).thenReturn(Optional.of(child));
        when(departmentRepository.findByParentId(rootId)).thenReturn(Collections.singletonList(child));
        when(departmentRepository.findByParentId(childId)).thenReturn(Collections.emptyList());

        // When & Then - Try to move root under its child
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.moveDepartment(rootId, childId));
        assertTrue(exception.getMessage().contains("Cannot move department to its descendant"));
    }

    @Test
    void shouldAssignManager() {
        // Given
        UUID departmentId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Department department = createTestDepartment(departmentId, "IT", "IT", null);
        User manager = createTestUser(managerId, "manager@example.com", "Manager", "User");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        // When
        departmentService.assignManager(departmentId, managerId);

        // Then
        assertEquals(manager, department.getManager());
    }

    @Test
    void shouldThrowExceptionWhenAssigningDeletedUserAsManager() {
        // Given
        UUID departmentId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Department department = createTestDepartment(departmentId, "IT", "IT", null);
        User manager = createTestUser(managerId, "manager@example.com", "Manager", "User");
        manager.softDelete();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.assignManager(departmentId, managerId));
        assertTrue(exception.getMessage().contains("Cannot assign deleted user as manager"));
    }

    @Test
    void shouldGetDescendantIds() {
        // Given
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID grandChildId = UUID.randomUUID();

        Department root = createTestDepartment(rootId, "Root", "ROOT", null);
        Department child = createTestDepartment(childId, "Child", "CHILD", root);
        Department grandChild = createTestDepartment(grandChildId, "GrandChild", "GC", child);

        when(departmentRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(departmentRepository.findByParentId(rootId)).thenReturn(Collections.singletonList(child));
        when(departmentRepository.findByParentId(childId)).thenReturn(Collections.singletonList(grandChild));
        when(departmentRepository.findByParentId(grandChildId)).thenReturn(Collections.emptyList());

        // When
        List<UUID> result = departmentService.getDescendantIds(rootId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(rootId));
        assertTrue(result.contains(childId));
        assertTrue(result.contains(grandChildId));
    }

    @Test
    void shouldReturnTrueWhenCodeIsAvailable() {
        // Given
        String code = "AVAILABLE";
        when(departmentRepository.findByCode(code)).thenReturn(Optional.empty());

        // When
        boolean result = departmentService.isCodeAvailable(code, null);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenCodeIsAvailableForOtherDepartment() {
        // Given
        String code = "CODE";
        UUID otherId = UUID.randomUUID();
        UUID excludeId = UUID.randomUUID();

        Department otherDept = createTestDepartment(otherId, "Other", code, null);

        when(departmentRepository.findByCode(code)).thenReturn(Optional.of(otherDept));

        // When - Same ID, should be available
        boolean result = departmentService.isCodeAvailable(code, otherId);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenCodeIsNotAvailable() {
        // Given
        String code = "TAKEN";
        UUID existingId = UUID.randomUUID();

        Department existingDept = createTestDepartment(existingId, "Existing", code, null);

        when(departmentRepository.findByCode(code)).thenReturn(Optional.of(existingDept));

        // When - Different excludeId, should not be available
        boolean result = departmentService.isCodeAvailable(code, UUID.randomUUID());

        // Then
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForEmptyCode() {
        // When
        boolean result = departmentService.isCodeAvailable("", null);

        // Then
        assertFalse(result);
        verify(departmentRepository, never()).findByCode(any());
    }

    // Helper methods
    private Department createTestDepartment(UUID id, String name, String code, Department parent) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode(code);
        department.setParent(parent);
        department.setLevel(parent != null ? parent.getLevel() + 1 : 1);
        department.setPath(parent != null ? parent.getPath() + "/" + id : "/" + id);
        department.setSortOrder(0);
        department.setStatus(Department.DepartmentStatus.ACTIVE);
        department.setCreatedAt(Instant.now());
        department.setUpdatedAt(Instant.now());
        return department;
    }

    private User createTestUser(UUID id, String email, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}
