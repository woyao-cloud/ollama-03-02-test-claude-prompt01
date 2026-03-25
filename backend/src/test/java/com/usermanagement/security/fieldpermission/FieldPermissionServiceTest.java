package com.usermanagement.security.fieldpermission;

import com.usermanagement.security.SecurityUtilsComponent;
import com.usermanagement.service.PermissionCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Field Permission Service Test
 *
 * @author Test Team
 * @since 1.0
 */
class FieldPermissionServiceTest {

    private FieldPermissionService fieldPermissionService;
    private PermissionCacheService permissionCacheService;
    private SecurityUtilsComponent securityUtils;

    @BeforeEach
    void setUp() {
        permissionCacheService = mock(PermissionCacheService.class);
        securityUtils = mock(SecurityUtilsComponent.class);

        fieldPermissionService = new FieldPermissionService(
                permissionCacheService,
                securityUtils
        );
    }

    @Test
    void shouldAllowAccessWhenUserHasPermission() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_SALARY_READ"))
                .thenReturn(true);

        // When
        boolean canAccess = fieldPermissionService.canReadField("user", "salary");

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldDenyAccessWhenUserDoesNotHavePermission() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_SALARY_READ"))
                .thenReturn(false);

        // When
        boolean canAccess = fieldPermissionService.canReadField("user", "salary");

        // Then
        assertFalse(canAccess);
    }

    @Test
    void shouldAllowWriteAccessWhenUserHasWritePermission() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_SALARY_WRITE"))
                .thenReturn(true);

        // When
        boolean canAccess = fieldPermissionService.canWriteField("user", "salary");

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldAllowOwnerToAccessTheirOwnData() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        // No permission granted, but owner should be allowed

        FieldPermission annotation = createFieldPermission("user", "phone", true, "id");

        // When - user accessing their own data
        boolean canAccess = fieldPermissionService.canAccessField(annotation, currentUserId);

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldDenyAccessToOthersDataWithoutPermission() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(any(), any())).thenReturn(false);

        FieldPermission annotation = createFieldPermission("user", "phone", true, "id");

        // When - user accessing other's data without permission
        boolean canAccess = fieldPermissionService.canAccessField(annotation, otherUserId);

        // Then
        assertFalse(canAccess);
    }

    @Test
    void shouldFilterFieldsOnObject() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_PASSWORD_READ"))
                .thenReturn(false);

        TestUserDTO dto = new TestUserDTO();
        dto.setId(currentUserId);
        dto.setName("John Doe");
        dto.setPassword("secret123");

        // When
        TestUserDTO filtered = fieldPermissionService.filterFields(dto);

        // Then
        assertEquals("John Doe", filtered.getName());
        assertNull(filtered.getPassword()); // Should be masked to null
    }

    @Test
    void shouldApplyAsteriskMask() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_PHONE_READ"))
                .thenReturn(false);

        TestUserDTO dto = new TestUserDTO();
        dto.setId(currentUserId);
        dto.setPhone("+1-555-1234");

        // When
        TestUserDTO filtered = fieldPermissionService.filterFields(dto);

        // Then
        assertTrue(filtered.getPhone().contains("****"));
    }

    @Test
    void shouldApplyPartialMaskToEmail() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_EMAIL_READ"))
                .thenReturn(false);

        TestUserDTO dto = new TestUserDTO();
        dto.setId(currentUserId);
        dto.setEmail("john.doe@example.com");

        // When
        TestUserDTO filtered = fieldPermissionService.filterFields(dto);

        // Then
        assertTrue(filtered.getEmail().startsWith("***"));
        assertTrue(filtered.getEmail().contains("@"));
    }

    @Test
    void shouldFilterListOfObjects() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(currentUserId, "USER_FIELD_SALARY_READ"))
                .thenReturn(false);

        List<TestUserDTO> users = Arrays.asList(
                createTestUser(UUID.randomUUID(), "User 1", 50000.0),
                createTestUser(UUID.randomUUID(), "User 2", 60000.0)
        );

        // When
        List<TestUserDTO> filtered = fieldPermissionService.filterList(users);

        // Then
        assertEquals(2, filtered.size());
        assertNull(filtered.get(0).getSalary());
        assertNull(filtered.get(1).getSalary());
    }

    @Test
    void shouldReturnEmptySetForNoAccessibleFields() {
        // Given - no permissions configured

        // When
        Set<String> fields = fieldPermissionService.getAccessibleFields("user", FieldPermission.AccessLevel.READ);

        // Then
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    void shouldHandleNullObject() {
        // When
        Object result = fieldPermissionService.filterFields(null);

        // Then
        assertNull(result);
    }

    @Test
    void shouldHandleObjectWithoutIdField() {
        // Given
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(permissionCacheService.hasPermission(any(), any())).thenReturn(false);

        Object simpleObj = new Object();

        // When
        Object result = fieldPermissionService.filterFields(simpleObj);

        // Then - should not throw exception
        assertNotNull(result);
    }

    // Helper methods

    private FieldPermission createFieldPermission(String resource, String field, boolean checkOwnership, String ownershipField) {
        return new FieldPermission() {
            @Override
            public String value() {
                return "";
            }

            @Override
            public String resource() {
                return resource;
            }

            @Override
            public String field() {
                return field;
            }

            @Override
            public AccessLevel access() {
                return AccessLevel.READ;
            }

            @Override
            public MaskType mask() {
                return MaskType.NULL;
            }

            @Override
            public String maskPattern() {
                return "***";
            }

            @Override
            public boolean checkOwnership() {
                return checkOwnership;
            }

            @Override
            public String ownershipField() {
                return ownershipField;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return FieldPermission.class;
            }
        };
    }

    private TestUserDTO createTestUser(UUID id, String name, Double salary) {
        TestUserDTO dto = new TestUserDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setSalary(salary);
        return dto;
    }

    // Test DTO class
    public static class TestUserDTO {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private Double salary;

        @FieldPermission(resource = "user", field = "password", mask = FieldPermission.MaskType.NULL)
        private String password;

        // Getters and Setters
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(Double salary) {
            this.salary = salary;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
