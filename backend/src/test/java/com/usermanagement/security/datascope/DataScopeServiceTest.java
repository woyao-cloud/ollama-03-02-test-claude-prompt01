package com.usermanagement.security.datascope;

import com.usermanagement.domain.entity.Department;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.User;
import com.usermanagement.domain.entity.UserRole;
import com.usermanagement.repository.DepartmentRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.SecurityUtilsComponent;
import com.usermanagement.service.DepartmentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Data Scope Service Test
 *
 * @author Test Team
 * @since 1.0
 */
class DataScopeServiceTest {

    private DataScopeService dataScopeService;
    private UserRepository userRepository;
    private DepartmentRepository departmentRepository;
    private DepartmentService departmentService;
    private SecurityUtilsComponent securityUtils;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        departmentService = mock(DepartmentService.class);
        securityUtils = mock(SecurityUtilsComponent.class);

        dataScopeService = new DataScopeService(
                userRepository,
                departmentRepository,
                departmentService,
                securityUtils
        );
    }

    @Test
    void shouldReturnAllScopeWhenUserHasAllScopeRole() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("ADMIN", Role.DataScope.ALL);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        DataScopeResult result = dataScopeService.getCurrentUserDataScope();

        // Then
        assertNotNull(result);
        assertEquals(Role.DataScope.ALL, result.getScope());
        assertTrue(result.isAll());
        assertTrue(result.canAccessAll());
    }

    @Test
    void shouldReturnSelfScopeWhenUserHasOnlySelfScopeRole() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("USER", Role.DataScope.SELF);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        DataScopeResult result = dataScopeService.getCurrentUserDataScope();

        // Then
        assertNotNull(result);
        assertEquals(Role.DataScope.SELF, result.getScope());
        assertTrue(result.isSelf());
        assertEquals(userId, result.getUserId());
    }

    @Test
    void shouldReturnDeptScopeWithDescendants() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID childDeptId = UUID.randomUUID();

        Department dept = createTestDepartment(deptId, "IT", "IT");
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        user.setDepartment(dept);

        Role role = createTestRole("DEPT_ADMIN", Role.DataScope.DEPT);
        user.addRole(role);

        List<UUID> descendants = Arrays.asList(deptId, childDeptId);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(departmentService.getDescendantIds(deptId)).thenReturn(descendants);

        // When
        DataScopeResult result = dataScopeService.getCurrentUserDataScope();

        // Then
        assertNotNull(result);
        assertEquals(Role.DataScope.DEPT, result.getScope());
        assertTrue(result.isDept());
        assertEquals(2, result.getDepartmentIds().size());
        assertTrue(result.getDepartmentIds().contains(deptId));
        assertTrue(result.getDepartmentIds().contains(childDeptId));
    }

    @Test
    void shouldReturnSelfScopeWhenUserHasNoDepartment() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        // No department set

        Role role = createTestRole("DEPT_ADMIN", Role.DataScope.DEPT);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        DataScopeResult result = dataScopeService.getCurrentUserDataScope();

        // Then
        assertNotNull(result);
        assertEquals(Role.DataScope.DEPT, result.getScope());
        // Should have empty department list but still be DEPT scope
        assertTrue(result.getDepartmentIds().isEmpty());
    }

    @Test
    void shouldReturnAllScopeWhenMultipleRolesWithDifferentScopes() {
        // Given - Multiple roles with different scopes, ALL should win
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");

        Role selfRole = createTestRole("USER", Role.DataScope.SELF);
        Role allRole = createTestRole("SUPER_ADMIN", Role.DataScope.ALL);
        user.addRole(selfRole);
        user.addRole(allRole);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        DataScopeResult result = dataScopeService.getCurrentUserDataScope();

        // Then
        assertNotNull(result);
        assertEquals(Role.DataScope.ALL, result.getScope());
    }

    @Test
    void shouldReturnSelfScopeAsDefaultWhenNoRoles() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        // No roles

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        DataScopeResult result = dataScopeService.getCurrentUserDataScope();

        // Then
        assertNotNull(result);
        assertEquals(Role.DataScope.SELF, result.getScope());
        assertEquals(userId, result.getUserId());
    }

    @Test
    void shouldAllowAccessToOwnDataWithSelfScope() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("USER", Role.DataScope.SELF);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean canAccess = dataScopeService.canAccessUserData(userId);

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldDenyAccessToOthersDataWithSelfScope() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("USER", Role.DataScope.SELF);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean canAccess = dataScopeService.canAccessUserData(otherUserId);

        // Then
        assertFalse(canAccess);
    }

    @Test
    void shouldAllowAccessToAllUsersWithAllScope() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("ADMIN", Role.DataScope.ALL);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean canAccess = dataScopeService.canAccessUserData(otherUserId);

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldAllowAccessToUsersInSameDepartment() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept = createTestDepartment(deptId, "IT", "IT");
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        user.setDepartment(dept);

        User otherUser = createTestUser(otherUserId, "other@example.com", "Other", "Test");
        otherUser.setDepartment(dept);

        Role role = createTestRole("DEPT_ADMIN", Role.DataScope.DEPT);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(departmentService.getDescendantIds(deptId)).thenReturn(Arrays.asList(deptId));

        // When
        boolean canAccess = dataScopeService.canAccessUserData(otherUserId);

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldDenyAccessToUsersInDifferentDepartment() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID otherDeptId = UUID.randomUUID();

        Department dept = createTestDepartment(deptId, "IT", "IT");
        Department otherDept = createTestDepartment(otherDeptId, "HR", "HR");

        User user = createTestUser(userId, "user@example.com", "User", "Test");
        user.setDepartment(dept);

        User otherUser = createTestUser(otherUserId, "other@example.com", "Other", "Test");
        otherUser.setDepartment(otherDept);

        Role role = createTestRole("DEPT_ADMIN", Role.DataScope.DEPT);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(departmentService.getDescendantIds(deptId)).thenReturn(Arrays.asList(deptId));

        // When
        boolean canAccess = dataScopeService.canAccessUserData(otherUserId);

        // Then
        assertFalse(canAccess);
    }

    @Test
    void shouldAllowAccessToAllDepartmentsWithAllScope() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("ADMIN", Role.DataScope.ALL);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean canAccess = dataScopeService.canAccessDepartmentData(deptId);

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldDenyAccessToDepartmentsWithSelfScope() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        User user = createTestUser(userId, "user@example.com", "User", "Test");
        Role role = createTestRole("USER", Role.DataScope.SELF);
        user.addRole(role);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean canAccess = dataScopeService.canAccessDepartmentData(deptId);

        // Then
        assertFalse(canAccess);
    }

    // Helper methods
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

    private Role createTestRole(String code, Role.DataScope dataScope) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(code);
        role.setCode(code);
        role.setDataScope(dataScope);
        role.setStatus(Role.RoleStatus.ACTIVE);
        return role;
    }

    private Department createTestDepartment(UUID id, String name, String code) {
        Department dept = new Department();
        dept.setId(id);
        dept.setName(name);
        dept.setCode(code);
        dept.setLevel(1);
        dept.setPath("/" + id);
        dept.setStatus(Department.DepartmentStatus.ACTIVE);
        return dept;
    }
}
