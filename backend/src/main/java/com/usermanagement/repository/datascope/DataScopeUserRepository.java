package com.usermanagement.repository.datascope;

import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.datascope.DataScopeContext;
import com.usermanagement.security.datascope.DataScopeResult;
import com.usermanagement.security.datascope.DataScopeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Data Scope User Repository
 * Wrapper around UserRepository that applies data scope filtering
 *
 * @author Repository Team
 * @since 1.0
 */
@Component
public class DataScopeUserRepository {

    private static final Logger logger = LoggerFactory.getLogger(DataScopeUserRepository.class);

    private final UserRepository userRepository;
    private final DataScopeService dataScopeService;

    public DataScopeUserRepository(UserRepository userRepository, DataScopeService dataScopeService) {
        this.userRepository = userRepository;
        this.dataScopeService = dataScopeService;
    }

    /**
     * Find all users with data scope filtering
     */
    public List<User> findAllWithDataScope() {
        DataScopeResult scope = dataScopeService.getCurrentUserDataScope();
        Specification<User> spec = buildDataScopeSpecification(scope);
        return userRepository.findAll(spec);
    }

    /**
     * Find users with data scope filtering and pagination
     */
    public Page<User> findAllWithDataScope(Pageable pageable) {
        DataScopeResult scope = dataScopeService.getCurrentUserDataScope();
        Specification<User> spec = buildDataScopeSpecification(scope);
        return userRepository.findAll(spec, pageable);
    }

    /**
     * Find users with additional specification and data scope filtering
     */
    public Page<User> findAllWithDataScope(Specification<User> spec, Pageable pageable) {
        DataScopeResult scope = dataScopeService.getCurrentUserDataScope();
        Specification<User> dataScopeSpec = buildDataScopeSpecification(scope);

        if (spec != null) {
            dataScopeSpec = spec.and(dataScopeSpec);
        }

        return userRepository.findAll(dataScopeSpec, pageable);
    }

    /**
     * Check if current user can access specific user
     */
    public boolean canAccess(UUID userId) {
        return dataScopeService.canAccessUserData(userId);
    }

    /**
     * Filter user IDs by data scope
     */
    public List<UUID> filterAccessibleUserIds(List<UUID> userIds) {
        DataScopeResult scope = dataScopeService.getCurrentUserDataScope();

        if (scope.isAll()) {
            return userIds;
        }

        List<UUID> accessible = new ArrayList<>();
        for (UUID userId : userIds) {
            if (dataScopeService.canAccessUserData(userId)) {
                accessible.add(userId);
            }
        }

        return accessible;
    }

    /**
     * Build JPA specification based on data scope
     */
    private Specification<User> buildDataScopeSpecification(DataScopeResult scope) {
        return (root, query, cb) -> {
            switch (scope.getScope()) {
                case ALL:
                    // No filtering needed
                    return cb.conjunction();

                case SELF:
                    // Only allow access to own data
                    if (scope.getUserId() != null) {
                        return cb.equal(root.get("id"), scope.getUserId());
                    }
                    return cb.disjunction();

                case DEPT:
                case CUSTOM:
                    // Allow access to users in specified departments
                    Set<UUID> deptIds = scope.getDepartmentIds();
                    if (deptIds != null && !deptIds.isEmpty()) {
                        return root.get("department").get("id").in(deptIds);
                    }
                    // No departments specified - fall back to self
                    if (scope.getUserId() != null) {
                        return cb.equal(root.get("id"), scope.getUserId());
                    }
                    return cb.disjunction();

                default:
                    // Unknown scope - deny all
                    return cb.disjunction();
            }
        };
    }

    /**
     * Execute a repository operation with data scope context
     */
    public <T> T executeWithDataScope(java.util.function.Function<UserRepository, T> operation) {
        try {
            // Setup data scope context
            DataScopeResult scope = dataScopeService.getCurrentUserDataScope();
            setupContext(scope);

            // Execute operation
            return operation.apply(userRepository);

        } finally {
            // Clear context
            DataScopeContext.clear();
        }
    }

    /**
     * Setup data scope context
     */
    private void setupContext(DataScopeResult scope) {
        DataScopeContext ctx = DataScopeContext.current();

        switch (scope.getScope()) {
            case ALL:
                ctx.allowAll();
                break;
            case SELF:
                ctx.allowUserId(scope.getUserId());
                break;
            case DEPT:
            case CUSTOM:
                ctx.allowUserId(scope.getUserId());
                ctx.allowDepartmentIds(scope.getDepartmentIds());
                break;
        }

        logger.debug("Data scope context set: scope={}, userId={}, deptCount={}",
                scope.getScope(), scope.getUserId(),
                scope.getDepartmentIds() != null ? scope.getDepartmentIds().size() : 0);
    }
}
