package com.usermanagement.security.datascope;

import com.usermanagement.domain.entity.Role;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Data Scope Result
 * Encapsulates the data scope evaluation result for a user
 *
 * @author Security Team
 * @since 1.0
 */
public class DataScopeResult {

    private final Role.DataScope scope;
    private final UUID userId;
    private final Set<UUID> departmentIds;

    private DataScopeResult(Role.DataScope scope, UUID userId, Set<UUID> departmentIds) {
        this.scope = scope;
        this.userId = userId;
        this.departmentIds = departmentIds != null ? new HashSet<>(departmentIds) : new HashSet<>();
    }

    /**
     * Create ALL scope result (can access all data)
     */
    public static DataScopeResult all() {
        return new DataScopeResult(Role.DataScope.ALL, null, null);
    }

    /**
     * Create SELF scope result (can only access own data)
     */
    public static DataScopeResult self(UUID userId) {
        return new DataScopeResult(Role.DataScope.SELF, userId, null);
    }

    /**
     * Create DEPT scope result (can access department and descendants)
     */
    public static DataScopeResult dept(UUID userId, Set<UUID> departmentIds) {
        return new DataScopeResult(Role.DataScope.DEPT, userId, departmentIds);
    }

    /**
     * Create CUSTOM scope result (custom department list)
     */
    public static DataScopeResult custom(UUID userId, Set<UUID> departmentIds) {
        return new DataScopeResult(Role.DataScope.CUSTOM, userId, departmentIds);
    }

    // Getters

    public Role.DataScope getScope() {
        return scope;
    }

    public UUID getUserId() {
        return userId;
    }

    public Set<UUID> getDepartmentIds() {
        return Collections.unmodifiableSet(departmentIds);
    }

    /**
     * Check if this is ALL scope
     */
    public boolean isAll() {
        return scope == Role.DataScope.ALL;
    }

    /**
     * Check if this is SELF scope
     */
    public boolean isSelf() {
        return scope == Role.DataScope.SELF;
    }

    /**
     * Check if this is DEPT scope
     */
    public boolean isDept() {
        return scope == Role.DataScope.DEPT;
    }

    /**
     * Check if this is CUSTOM scope
     */
    public boolean isCustom() {
        return scope == Role.DataScope.CUSTOM;
    }

    /**
     * Check if user can access all data
     */
    public boolean canAccessAll() {
        return isAll();
    }

    @Override
    public String toString() {
        return "DataScopeResult{" +
                "scope=" + scope +
                ", userId=" + userId +
                ", departmentCount=" + departmentIds.size() +
                '}';
    }
}
