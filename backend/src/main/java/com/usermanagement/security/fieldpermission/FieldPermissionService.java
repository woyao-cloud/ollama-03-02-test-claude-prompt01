package com.usermanagement.security.fieldpermission;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.usermanagement.security.SecurityUtilsComponent;
import com.usermanagement.service.PermissionCacheService;

/**
 * Field Permission Service
 * Evaluates and enforces field-level permissions
 *
 * @author Security Team
 * @since 1.0
 */
@Service
public class FieldPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(FieldPermissionService.class);

    private final PermissionCacheService permissionCacheService;
    private final SecurityUtilsComponent securityUtils;

    public FieldPermissionService(PermissionCacheService permissionCacheService,
                                   SecurityUtilsComponent securityUtils) {
        this.permissionCacheService = permissionCacheService;
        this.securityUtils = securityUtils;
    }

    /**
     * Check if current user can access a field
     *
     * @param resource resource name (e.g., "user")
     * @param field field name (e.g., "salary")
     * @param accessLevel required access level
     * @return true if allowed
     */
    public boolean canAccessField(String resource, String field, FieldPermission.AccessLevel accessLevel) {
        String permissionCode = buildFieldPermissionCode(resource, field, accessLevel);
        return hasFieldPermission(permissionCode);
    }

    /**
     * Check if current user can read a field
     */
    public boolean canReadField(String resource, String field) {
        return canAccessField(resource, field, FieldPermission.AccessLevel.READ);
    }

    /**
     * Check if current user can write a field
     */
    public boolean canWriteField(String resource, String field) {
        return canAccessField(resource, field, FieldPermission.AccessLevel.WRITE);
    }

    /**
     * Check field permission using annotation
     */
    public boolean canAccessField(FieldPermission annotation, UUID targetUserId) {
        if (annotation == null) {
            return true; // No annotation, allow access
        }

        String permissionCode = getPermissionCode(annotation);

        // Check ownership first if enabled
        if (annotation.checkOwnership() && targetUserId != null) {
            try {
                UUID currentUserId = securityUtils.getCurrentUserId().orElse(null);
                if (currentUserId != null && currentUserId.equals(targetUserId)) {
                    return true; // Owner can access their own data
                }
            } catch (Exception e) {
                logger.debug("Failed to check ownership: {}", e.getMessage());
            }
        }

        return hasFieldPermission(permissionCode);
    }

    /**
     * Filter object fields based on permissions
     *
     * @param obj object to filter
     * @param targetUserId target user ID for ownership check
     * @return filtered object (same instance with modified fields)
     */
    public <T> T filterFields(T obj, UUID targetUserId) {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            FieldPermission annotation = field.getAnnotation(FieldPermission.class);
            if (annotation != null) {
                if (!canAccessField(annotation, targetUserId)) {
                    applyMask(obj, field, annotation);
                }
            }
        }

        return obj;
    }

    /**
     * Filter object fields using default ownership check
     */
    public <T> T filterFields(T obj) {
        UUID targetUserId = extractUserId(obj);
        return filterFields(obj, targetUserId);
    }

    /**
     * Filter a list of objects
     */
    public <T> List<T> filterList(List<T> list, UUID targetUserId) {
        if (list == null) {
            return null;
        }

        List<T> filtered = new ArrayList<>();
        for (T item : list) {
            filtered.add(filterFields(item, targetUserId));
        }
        return filtered;
    }

    /**
     * Filter a list with auto-detected ownership
     */
    public <T> List<T> filterList(List<T> list) {
        if (list == null) {
            return null;
        }

        List<T> filtered = new ArrayList<>();
        for (T item : list) {
            filtered.add(filterFields(item));
        }
        return filtered;
    }

    /**
     * Get all accessible fields for a resource
     *
     * @param resource resource name
     * @param accessLevel access level
     * @return set of accessible field names
     */
    public Set<String> getAccessibleFields(String resource, FieldPermission.AccessLevel accessLevel) {
        Set<String> accessibleFields = new HashSet<>();
        // This would typically query the permission system for FIELD type permissions
        // For now, return empty set - implementation would depend on permission storage
        return accessibleFields;
    }

    /**
     * Check if user has specific field permission
     */
    private boolean hasFieldPermission(String permissionCode) {
        try {
            UUID currentUserId = securityUtils.getCurrentUserId().orElse(null);
            if (currentUserId == null) {
                return false;
            }
            return permissionCacheService.hasPermission(currentUserId, permissionCode);
        } catch (Exception e) {
            logger.debug("Permission check failed for {}: {}", permissionCode, e.getMessage());
            return false;
        }
    }

    /**
     * Build permission code from annotation
     */
    private String getPermissionCode(FieldPermission annotation) {
        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }

        return buildFieldPermissionCode(annotation.resource(), annotation.field(), annotation.access());
    }

    /**
     * Build standard field permission code
     */
    private String buildFieldPermissionCode(String resource, String field, FieldPermission.AccessLevel accessLevel) {
        String action = accessLevel == FieldPermission.AccessLevel.WRITE ? "WRITE" : "READ";
        return resource.toUpperCase() + "_FIELD_" + field.toUpperCase() + "_" + action;
    }

    /**
     * Apply mask to field based on annotation
     */
    private void applyMask(Object obj, Field field, FieldPermission annotation) {
        field.setAccessible(true);

        try {
            Object maskedValue = createMaskedValue(field.get(obj), annotation);
            field.set(obj, maskedValue);

            logger.debug("Applied {} mask to field {}", annotation.mask(), field.getName());
        } catch (IllegalAccessException e) {
            logger.warn("Failed to mask field {}: {}", field.getName(), e.getMessage());
        }
    }

    /**
     * Create masked value based on mask type
     */
    private Object createMaskedValue(Object originalValue, FieldPermission annotation) {
        if (originalValue == null) {
            return null;
        }

        switch (annotation.mask()) {
            case NULL:
                return null;

            case EMPTY:
                if (originalValue instanceof String) {
                    return "";
                } else if (originalValue instanceof Collection) {
                    return new ArrayList<>();
                } else if (originalValue instanceof Map) {
                    return new HashMap<>();
                }
                return null;

            case ASTERISK:
                if (originalValue instanceof String) {
                    String str = (String) originalValue;
                    return str.length() <= 4 ? "****" : "****" + str.substring(str.length() - 4);
                }
                return "****";

            case PARTIAL:
                return applyPartialMask(originalValue);

            case CUSTOM:
                return annotation.maskPattern();

            case HIDE:
                return null; // Will be handled by JSON serializer

            default:
                return null;
        }
    }

    /**
     * Apply partial masking (e.g., ***@example.com)
     */
    private Object applyPartialMask(Object value) {
        if (!(value instanceof String)) {
            return "****";
        }

        String str = (String) value;

        // Email masking
        if (str.contains("@")) {
            int atIndex = str.indexOf('@');
            if (atIndex > 2) {
                return "***" + str.substring(atIndex);
            }
            return "***" + str.substring(atIndex);
        }

        // Phone masking
        if (str.matches("^[\\d\\-\\+\\s]+$")) {
            if (str.length() > 4) {
                return "****" + str.substring(str.length() - 4);
            }
            return "****";
        }

        // General string masking
        if (str.length() > 4) {
            return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
        }

        return "****";
    }

    /**
     * Extract user ID from object if possible
     */
    private UUID extractUserId(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // Try to get id field
            Field idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(obj);

            if (id instanceof UUID) {
                return (UUID) id;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.debug("Could not extract user ID from object: {}", e.getMessage());
        }

        return null;
    }
}
