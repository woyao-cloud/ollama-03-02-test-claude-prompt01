package com.usermanagement.security.datascope;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.util.*;

/**
 * Data Scope Utility
 * Helper methods for applying data scope filtering in queries
 *
 * @author Security Team
 * @since 1.0
 */
public class DataScopeUtil {

    /**
     * Apply data scope to a JPA Specification
     *
     * @param <T> entity type
     * @param spec existing specification (can be null)
     * @return combined specification with data scope
     */
    public static <T> Specification<T> applyDataScope(Specification<T> spec) {
        return applyDataScope(spec, "user.id", "department.id");
    }

    /**
     * Apply data scope to a JPA Specification with custom field paths
     *
     * @param <T> entity type
     * @param spec existing specification (can be null)
     * @param userIdField path to user ID field
     * @param deptIdField path to department ID field
     * @return combined specification with data scope
     */
    public static <T> Specification<T> applyDataScope(Specification<T> spec, String userIdField, String deptIdField) {
        Specification<T> dataScopeSpec = buildDataScopeSpecification(userIdField, deptIdField);

        if (spec == null) {
            return dataScopeSpec;
        }
        return spec.and(dataScopeSpec);
    }

    /**
     * Build a specification based on current data scope context
     */
    private static <T> Specification<T> buildDataScopeSpecification(String userIdField, String deptIdField) {
        return (root, query, cb) -> {
            // Check if context exists
            if (!DataScopeContext.exists()) {
                // No data scope context - allow all
                return cb.conjunction();
            }

            DataScopeContext context = DataScopeContext.current();

            // ALL scope - no filtering
            if (context.isAllAllowed()) {
                return cb.conjunction();
            }

            // Build predicates
            List<Predicate> predicates = new ArrayList<>();

            // Add user ID filter
            Set<UUID> allowedUserIds = context.getAllowedUserIds();
            if (!allowedUserIds.isEmpty()) {
                Path<UUID> userIdPath = getPath(root, userIdField);
                predicates.add(userIdPath.in(allowedUserIds));
            }

            // Add department ID filter
            Set<UUID> allowedDeptIds = context.getAllowedDepartmentIds();
            if (!allowedDeptIds.isEmpty()) {
                Path<UUID> deptIdPath = getPath(root, deptIdField);
                predicates.add(deptIdPath.in(allowedDeptIds));
            }

            // No filters applied - allow all (or deny all based on requirements)
            if (predicates.isEmpty()) {
                // Default to deny if context exists but has no filters
                return cb.disjunction();
            }

            // Combine predicates with OR
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Get a path from root by field path (supports nested paths like "user.department.id")
     */
    @SuppressWarnings("unchecked")
    private static <T> Path<T> getPath(Root<?> root, String path) {
        String[] parts = path.split("\\.");
        Path<?> currentPath = root.get(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            currentPath = currentPath.get(parts[i]);
        }

        return (Path<T>) currentPath;
    }

    /**
     * Check if data scope filtering is active
     */
    public static boolean isDataScopeActive() {
        return DataScopeContext.exists() && !DataScopeContext.current().isAllAllowed();
    }

    /**
     * Check if current user can access the given user ID
     */
    public static boolean canAccessUser(UUID userId) {
        if (!DataScopeContext.exists()) {
            return true; // No context, allow by default
        }
        return DataScopeContext.current().isUserAllowed(userId);
    }

    /**
     * Check if current user can access the given department ID
     */
    public static boolean canAccessDepartment(UUID deptId) {
        if (!DataScopeContext.exists()) {
            return true; // No context, allow by default
        }
        return DataScopeContext.current().isDepartmentAllowed(deptId);
    }

    /**
     * Filter a list based on data scope (for in-memory filtering)
     */
    public static <T> List<T> filterList(List<T> list, DataScopeFilter<T> filter) {
        if (!DataScopeContext.exists() || DataScopeContext.current().isAllAllowed()) {
            return list;
        }

        DataScopeContext context = DataScopeContext.current();
        List<T> filtered = new ArrayList<>();

        for (T item : list) {
            if (filter.isAllowed(item, context)) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    /**
     * Functional interface for filtering items
     */
    @FunctionalInterface
    public interface DataScopeFilter<T> {
        boolean isAllowed(T item, DataScopeContext context);
    }
}
