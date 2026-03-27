package com.usermanagement.aop;

import com.usermanagement.domain.entity.AuditLog.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Audit Log Annotation
 * Marks methods that should be audited
 *
 * @author AOP Team
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * Operation type
     */
    OperationType operation();

    /**
     * Resource type (e.g., "USER", "ROLE", "PERMISSION")
     */
    String resourceType();

    /**
     * Description template (supports SpEL)
     */
    String description() default "";

    /**
     * Whether to log method parameters
     */
    boolean logParams() default true;

    /**
     * Whether to log method return value
     */
    boolean logResult() default true;

    /**
     * Parameter names to include in audit (empty means all)
     */
    String[] includeParams() default {};

    /**
     * Parameter names to exclude from audit
     */
    String[] excludeParams() default {"password", "currentPassword", "newPassword"};
}
