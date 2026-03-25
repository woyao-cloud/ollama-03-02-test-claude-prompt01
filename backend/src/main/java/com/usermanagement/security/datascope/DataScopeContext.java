package com.usermanagement.security.datascope;

import java.util.*;

/**
 * Data Scope Context
 * Thread-local context for passing data scope parameters across layers
 *
 * @author Security Team
 * @since 1.0
 */
public class DataScopeContext {

    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    private final Set<UUID> allowedUserIds = new HashSet<>();
    private final Set<UUID> allowedDepartmentIds = new HashSet<>();
    private boolean allAllowed = false;
    private String userIdField = "user.id";
    private String deptIdField = "department.id";

    private DataScopeContext() {}

    /**
     * Get current context (creates if not exists)
     */
    public static DataScopeContext current() {
        DataScopeContext ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new DataScopeContext();
            CONTEXT.set(ctx);
        }
        return ctx;
    }

    /**
     * Clear current context
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Check if context exists
     */
    public static boolean exists() {
        return CONTEXT.get() != null;
    }

    // Setters

    public DataScopeContext allowAll() {
        this.allAllowed = true;
        return this;
    }

    public DataScopeContext allowUserId(UUID userId) {
        this.allowedUserIds.add(userId);
        return this;
    }

    public DataScopeContext allowUserIds(Collection<UUID> userIds) {
        this.allowedUserIds.addAll(userIds);
        return this;
    }

    public DataScopeContext allowDepartmentId(UUID deptId) {
        this.allowedDepartmentIds.add(deptId);
        return this;
    }

    public DataScopeContext allowDepartmentIds(Collection<UUID> deptIds) {
        this.allowedDepartmentIds.addAll(deptIds);
        return this;
    }

    public DataScopeContext withUserIdField(String field) {
        this.userIdField = field;
        return this;
    }

    public DataScopeContext withDeptIdField(String field) {
        this.deptIdField = field;
        return this;
    }

    // Getters

    public boolean isAllAllowed() {
        return allAllowed;
    }

    public Set<UUID> getAllowedUserIds() {
        return Collections.unmodifiableSet(allowedUserIds);
    }

    public Set<UUID> getAllowedDepartmentIds() {
        return Collections.unmodifiableSet(allowedDepartmentIds);
    }

    public String getUserIdField() {
        return userIdField;
    }

    public String getDeptIdField() {
        return deptIdField;
    }

    /**
     * Check if user ID is allowed
     */
    public boolean isUserAllowed(UUID userId) {
        if (allAllowed) return true;
        return allowedUserIds.contains(userId);
    }

    /**
     * Check if department ID is allowed
     */
    public boolean isDepartmentAllowed(UUID deptId) {
        if (allAllowed) return true;
        return allowedDepartmentIds.contains(deptId);
    }

    /**
     * Check if any restrictions are applied
     */
    public boolean hasRestrictions() {
        return !allAllowed && (allowedUserIds.isEmpty() && allowedDepartmentIds.isEmpty());
    }

    @Override
    public String toString() {
        return "DataScopeContext{" +
                "allAllowed=" + allAllowed +
                ", allowedUserIds=" + allowedUserIds.size() +
                ", allowedDepartmentIds=" + allowedDepartmentIds.size() +
                ", userIdField='" + userIdField + '\'' +
                ", deptIdField='" + deptIdField + '\'' +
                '}';
    }
}
