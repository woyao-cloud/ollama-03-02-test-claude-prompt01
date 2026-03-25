package com.usermanagement.security.datascope;

import java.lang.annotation.*;

/**
 * Data Scope Annotation
 * Marks a method or class to apply data scope filtering
 *
 * @author Security Team
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * Path to user ID field in the entity being queried
     * Used for SELF scope filtering
     * Default: "user.id"
     */
    String userIdField() default "user.id";

    /**
     * Path to department ID field in the entity being queried
     * Used for DEPT and CUSTOM scope filtering
     * Default: "department.id"
     */
    String deptIdField() default "department.id";

    /**
     * Whether to ignore data scope filtering
     * Useful for admin methods that need to bypass filtering
     */
    boolean ignore() default false;

    /**
     * Scope types to apply (if empty, all scopes are evaluated)
     * Can be used to restrict which scope types are checked
     */
    String[] scopeTypes() default {};
}
