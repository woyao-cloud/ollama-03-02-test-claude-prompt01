package com.usermanagement.security.fieldpermission;

import java.lang.annotation.*;

/**
 * Field Permission Annotation
 * Marks a field or method that requires field-level permission control
 *
 * @author Security Team
 * @since 1.0
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldPermission {

    /**
     * Permission code required to access this field
     * If empty, uses resource:field format
     */
    String value() default "";

    /**
     * Resource name (e.g., "user", "department")
     */
    String resource() default "";

    /**
     * Field name (e.g., "salary", "password")
     */
    String field() default "";

    /**
     * Access level: READ (view only) or WRITE (view and edit)
     */
    AccessLevel access() default AccessLevel.READ;

    /**
     * Masking strategy for sensitive fields when access is denied
     */
    MaskType mask() default MaskType.NULL;

    /**
     * Custom mask pattern (used when mask = CUSTOM)
     */
    String maskPattern() default "***";

    /**
     * Whether to check ownership (allow owner to access their own data)
     */
    boolean checkOwnership() default true;

    /**
     * Ownership field path (e.g., "id" for user owns their own data)
     */
    String ownershipField() default "id";

    /**
     * Access level enum
     */
    enum AccessLevel {
        READ,   // Can view the field
        WRITE   // Can view and modify the field
    }

    /**
     * Mask type enum
     */
    enum MaskType {
        NULL,       // Set to null
        EMPTY,      // Set to empty string/collection
        ASTERISK,   // Replace with ****
        PARTIAL,    // Partial masking (e.g., ***@example.com)
        CUSTOM,     // Use custom mask pattern
        HIDE        // Remove field from JSON entirely
    }
}
