package com.usermanagement.security.datascope;

import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.DepartmentRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.SecurityUtilsComponent;
import com.usermanagement.service.DepartmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Data Scope Service
 * Evaluates and provides data filtering criteria based on user's role data scopes
 *
 * @author Security Team
 * @since 1.0
 */
@Service
public class DataScopeService {

    private static final Logger logger = LoggerFactory.getLogger(DataScopeService.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;
    private final SecurityUtilsComponent securityUtils;

    public DataScopeService(UserRepository userRepository,
                            DepartmentRepository departmentRepository,
                            DepartmentService departmentService,
                            SecurityUtilsComponent securityUtils) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.departmentService = departmentService;
        this.securityUtils = securityUtils;
    }

    /**
     * Get data scope for current user
     *
     * @return DataScopeResult containing scope type and applicable IDs
     */
    public DataScopeResult getCurrentUserDataScope() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return getUserDataScope(currentUserId);
    }

    /**
     * Get data scope for a specific user
     *
     * @param userId user ID
     * @return DataScopeResult containing scope type and applicable IDs
     */
    public DataScopeResult getUserDataScope(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // If user has no roles, default to SELF scope
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return DataScopeResult.self(userId);
        }

        // Collect all data scopes from user's roles
        Set<Role.DataScope> scopes = new HashSet<>();
        for (var userRole : user.getUserRoles()) {
            Role role = userRole.getRole();
            if (role != null && role.isActive()) {
                scopes.add(role.getDataScope());
            }
        }

        // Determine effective scope (most permissive wins)
        // Priority: ALL > CUSTOM > DEPT > SELF
        if (scopes.contains(Role.DataScope.ALL)) {
            return DataScopeResult.all();
        }

        if (scopes.contains(Role.DataScope.CUSTOM)) {
            // For custom scope, we would need custom department mappings
            // For now, fall back to DEPT scope
            return getDeptScope(user);
        }

        if (scopes.contains(Role.DataScope.DEPT)) {
            return getDeptScope(user);
        }

        // Default to SELF
        return DataScopeResult.self(userId);
    }

    /**
     * Get department scope for user (includes user's department and all descendants)
     */
    private DataScopeResult getDeptScope(User user) {
        Set<UUID> departmentIds = new HashSet<>();

        if (user.getDepartment() != null) {
            UUID userDeptId = user.getDepartment().getId();
            departmentIds.add(userDeptId);

            // Get all descendant departments
            try {
                List<UUID> descendants = departmentService.getDescendantIds(userDeptId);
                departmentIds.addAll(descendants);
            } catch (Exception e) {
                logger.warn("Failed to get descendant departments for user {}: {}", user.getId(), e.getMessage());
            }
        }

        return DataScopeResult.dept(user.getId(), departmentIds);
    }

    /**
     * Check if current user can access data for the given user ID
     *
     * @param targetUserId the user ID to check access for
     * @return true if accessible
     */
    public boolean canAccessUserData(UUID targetUserId) {
        DataScopeResult scope = getCurrentUserDataScope();

        // ALL scope can access everything
        if (scope.getScope() == Role.DataScope.ALL) {
            return true;
        }

        // SELF scope can only access own data
        if (scope.getScope() == Role.DataScope.SELF) {
            return scope.getUserId().equals(targetUserId);
        }

        // DEPT scope can access users in same department
        if (scope.getScope() == Role.DataScope.DEPT) {
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null || targetUser.isDeleted()) {
                return false;
            }

            if (targetUser.getDepartment() == null) {
                return false;
            }

            return scope.getDepartmentIds().contains(targetUser.getDepartment().getId());
        }

        // CUSTOM scope - check custom rules
        if (scope.getScope() == Role.DataScope.CUSTOM) {
            // Custom logic would be implemented here
            return canAccessCustomScope(scope, targetUserId);
        }

        return false;
    }

    /**
     * Check if current user can access data for the given department ID
     *
     * @param departmentId the department ID to check access for
     * @return true if accessible
     */
    public boolean canAccessDepartmentData(UUID departmentId) {
        DataScopeResult scope = getCurrentUserDataScope();

        // ALL scope can access everything
        if (scope.getScope() == Role.DataScope.ALL) {
            return true;
        }

        // SELF scope cannot access department-level data
        if (scope.getScope() == Role.DataScope.SELF) {
            return false;
        }

        // DEPT scope can access specified departments and their descendants
        if (scope.getScope() == Role.DataScope.DEPT) {
            return scope.getDepartmentIds().contains(departmentId);
        }

        // CUSTOM scope
        if (scope.getScope() == Role.DataScope.CUSTOM) {
            return canAccessCustomDepartmentScope(scope, departmentId);
        }

        return false;
    }

    /**
     * Get filter criteria for current query based on data scope
     * Returns a map of filter conditions to apply
     *
     * @param userIdPath path to user ID field (e.g., "user.id")
     * @param deptIdPath path to department ID field (e.g., "user.department.id")
     * @return filter conditions map
     */
    public Map<String, Object> getFilterCriteria(String userIdPath, String deptIdPath) {
        DataScopeResult scope = getCurrentUserDataScope();
        Map<String, Object> filters = new HashMap<>();

        switch (scope.getScope()) {
            case ALL:
                // No filters needed
                break;
            case SELF:
                filters.put(userIdPath, scope.getUserId());
                break;
            case DEPT:
                if (deptIdPath != null && !scope.getDepartmentIds().isEmpty()) {
                    filters.put(deptIdPath, scope.getDepartmentIds());
                }
                break;
            case CUSTOM:
                // Custom filter logic
                filters.putAll(getCustomFilterCriteria(scope, userIdPath, deptIdPath));
                break;
        }

        return filters;
    }

    /**
     * Check custom scope access (placeholder for extension)
     */
    private boolean canAccessCustomScope(DataScopeResult scope, UUID targetUserId) {
        // Custom implementation would check custom department mappings
        // For now, delegate to DEPT logic
        return canAccessDepartmentDataForUser(scope, targetUserId);
    }

    /**
     * Check custom department scope access
     */
    private boolean canAccessCustomDepartmentScope(DataScopeResult scope, UUID departmentId) {
        // Custom implementation would check custom department mappings
        return scope.getDepartmentIds().contains(departmentId);
    }

    /**
     * Get custom filter criteria
     */
    private Map<String, Object> getCustomFilterCriteria(DataScopeResult scope, String userIdPath, String deptIdPath) {
        Map<String, Object> filters = new HashMap<>();
        // Custom implementation
        if (deptIdPath != null && !scope.getDepartmentIds().isEmpty()) {
            filters.put(deptIdPath, scope.getDepartmentIds());
        }
        return filters;
    }

    /**
     * Check if user can be accessed based on department scope
     */
    private boolean canAccessDepartmentDataForUser(DataScopeResult scope, UUID targetUserId) {
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null || targetUser.isDeleted()) {
            return false;
        }

        if (targetUser.getDepartment() == null) {
            return false;
        }

        return scope.getDepartmentIds().contains(targetUser.getDepartment().getId());
    }
}
