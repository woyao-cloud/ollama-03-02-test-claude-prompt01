package com.usermanagement.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.domain.entity.AuditLog.OperationType;
import com.usermanagement.service.AuditLogService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Audit Aspect
 * Intercepts methods annotated with @AuditLog and records audit information
 *
 * @author AOP Team
 * @since 1.0
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser parser;

    public AuditAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.parser = new SpelExpressionParser();
    }

    @Around("@annotation(com.usermanagement.aop.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditAnnotation = method.getAnnotation(AuditLog.class);

        Instant startTime = Instant.now();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            recordAudit(joinPoint, auditAnnotation, result, exception, executionTime);
        }
    }

    private void recordAudit(ProceedingJoinPoint joinPoint, AuditLog auditAnnotation,
                             Object result, Exception exception, long executionTime) {
        try {
            OperationType operation = auditAnnotation.operation();
            String resourceType = auditAnnotation.resourceType();
            UUID resourceId = extractResourceId(joinPoint, result, resourceType);

            // Build description
            String description = buildDescription(auditAnnotation.description(), joinPoint, result);

            // Build parameter map
            Map<String, Object> params = auditAnnotation.logParams()
                    ? extractParameters(joinPoint, auditAnnotation)
                    : null;

            // Build result map
            Map<String, Object> resultMap = auditAnnotation.logResult() && result != null
                    ? convertToMap(result)
                    : null;

            boolean success = exception == null;
            String errorMessage = exception != null ? exception.getMessage() : null;

            auditLogService.logOperation(
                    operation,
                    resourceType,
                    resourceId,
                    description,
                    params,
                    resultMap,
                    success,
                    errorMessage
            );

        } catch (Exception e) {
            logger.error("Failed to record audit log", e);
        }
    }

    private UUID extractResourceId(ProceedingJoinPoint joinPoint, Object result, String resourceType) {
        Object[] args = joinPoint.getArgs();

        // Try to find ID from parameters
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }

        // Try to extract from result
        if (result != null) {
            try {
                if (result instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) result;
                    Object id = map.get("id");
                    if (id instanceof UUID) {
                        return (UUID) id;
                    }
                }

                // Try to get id field via reflection
                java.lang.reflect.Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof UUID) {
                    return (UUID) id;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return null;
    }

    private String buildDescription(String descriptionTemplate, ProceedingJoinPoint joinPoint, Object result) {
        if (descriptionTemplate == null || descriptionTemplate.isEmpty()) {
            return null;
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Add method arguments
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            // Add result
            context.setVariable("result", result);

            return parser.parseExpression(descriptionTemplate).getValue(context, String.class);
        } catch (Exception e) {
            logger.warn("Failed to parse description template: {}", descriptionTemplate, e);
            return descriptionTemplate;
        }
    }

    private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint, AuditLog auditAnnotation) {
        Map<String, Object> params = new HashMap<>();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        Set<String> includeSet = new HashSet<>(Arrays.asList(auditAnnotation.includeParams()));
        Set<String> excludeSet = new HashSet<>(Arrays.asList(auditAnnotation.excludeParams()));

        boolean includeAll = includeSet.isEmpty();

        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];

            // Skip excluded parameters
            if (excludeSet.contains(paramName)) {
                continue;
            }

            // Only include specified parameters if include list is not empty
            if (!includeAll && !includeSet.contains(paramName)) {
                continue;
            }

            params.put(paramName, args[i]);
        }

        return params;
    }

    private Map<String, Object> convertToMap(Object object) {
        try {
            if (object == null) {
                return null;
            }

            String json = objectMapper.writeValueAsString(object);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            // Remove sensitive fields
            map.remove("password");
            map.remove("passwordHash");
            map.remove("token");
            map.remove("secret");

            return map;
        } catch (Exception e) {
            logger.warn("Failed to convert object to map", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("toString", object.toString());
            return fallback;
        }
    }
}
