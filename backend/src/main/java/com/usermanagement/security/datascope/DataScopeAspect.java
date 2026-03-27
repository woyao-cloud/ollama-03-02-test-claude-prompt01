package com.usermanagement.security.datascope;

import com.usermanagement.domain.entity.Role;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Data Scope Aspect
 * AOP aspect that intercepts methods annotated with @DataScope
 * and sets up the data scope context for filtering
 *
 * @author Security Team
 * @since 1.0
 */
@Aspect
@Component
public class DataScopeAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataScopeAspect.class);

    private final DataScopeService dataScopeService;

    public DataScopeAspect(DataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    /**
     * Intercept methods annotated with @DataScope
     */
    @Before("@annotation(dataScope)")
    public void before(JoinPoint joinPoint, DataScope dataScope) {
        if (dataScope.ignore()) {
            logger.debug("Data scope ignored for method: {}", joinPoint.getSignature().getName());
            return;
        }

        logger.debug("Applying data scope for method: {}", joinPoint.getSignature().getName());

        try {
            // Clear any existing context
            DataScopeContext.clear();

            // Get current user's data scope
            DataScopeResult scopeResult = dataScopeService.getCurrentUserDataScope();

            // Setup context based on scope
            setupContext(scopeResult, dataScope);

        } catch (Exception e) {
            logger.error("Failed to apply data scope", e);
            // Continue without data scope filtering
        }
    }

    /**
     * Setup data scope context based on scope result
     */
    private void setupContext(DataScopeResult scopeResult, DataScope dataScope) {
        DataScopeContext context = DataScopeContext.current()
                .withUserIdField(dataScope.userIdField())
                .withDeptIdField(dataScope.deptIdField());

        switch (scopeResult.getScope()) {
            case ALL:
                context.allowAll();
                logger.debug("Data scope: ALL - no filtering applied");
                break;

            case SELF:
                context.allowUserId(scopeResult.getUserId());
                logger.debug("Data scope: SELF - userId={}", scopeResult.getUserId());
                break;

            case DEPT:
            case CUSTOM:
                Set<UUID> deptIds = scopeResult.getDepartmentIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    context.allowDepartmentIds(deptIds);
                    logger.debug("Data scope: {} - departments={}",
                            scopeResult.getScope(), deptIds.size());
                } else {
                    // No departments, default to self
                    context.allowUserId(scopeResult.getUserId());
                    logger.debug("Data scope: {} - no departments, fallback to SELF", scopeResult.getScope());
                }
                break;
        }
    }

    /**
     * Check if specific scope type should be applied
     */
    private boolean shouldApplyScope(String[] scopeTypes, Role.DataScope currentScope) {
        if (scopeTypes == null || scopeTypes.length == 0) {
            return true; // Apply all scopes if not specified
        }

        for (String scopeType : scopeTypes) {
            if (currentScope.name().equalsIgnoreCase(scopeType)) {
                return true;
            }
        }
        return false;
    }
}
