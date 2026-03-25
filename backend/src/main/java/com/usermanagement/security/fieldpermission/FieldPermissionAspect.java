package com.usermanagement.security.fieldpermission;

import com.usermanagement.security.SecurityUtilsComponent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Field Permission Aspect
 * AOP aspect that filters response objects based on field-level permissions
 *
 * @author Security Team
 * @since 1.0
 */
@Aspect
@Component
public class FieldPermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(FieldPermissionAspect.class);

    private final FieldPermissionService fieldPermissionService;
    private final SecurityUtilsComponent securityUtils;

    public FieldPermissionAspect(FieldPermissionService fieldPermissionService,
                                   SecurityUtilsComponent securityUtils) {
        this.fieldPermissionService = fieldPermissionService;
        this.securityUtils = securityUtils;
    }

    /**
     * Intercept controller methods returning ResponseEntity
     * Applies field filtering to the response body
     */
    @Around("execution(public org.springframework.http.ResponseEntity com.usermanagement.web.controller..*.*(..))")
    public Object filterResponseFields(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            Object body = responseEntity.getBody();

            if (body != null) {
                Object filteredBody = filterBody(body);
                if (filteredBody != body) {
                    return ResponseEntity.status(responseEntity.getStatusCode())
                            .headers(responseEntity.getHeaders())
                            .body(filteredBody);
                }
            }
        }

        return result;
    }

    /**
     * Filter response body based on type
     */
    private Object filterBody(Object body) {
        try {
            // Handle ApiResponse wrapper
            if (body instanceof com.usermanagement.web.dto.ApiResponse) {
                return filterApiResponse((com.usermanagement.web.dto.ApiResponse<?>) body);
            }

            // Handle single object
            if (isDtoObject(body)) {
                return fieldPermissionService.filterFields(body);
            }

            // Handle collections
            if (body instanceof Collection) {
                return fieldPermissionService.filterList((List<?>) body);
            }

            // Handle Page
            if (body instanceof Page) {
                return filterPage((Page<?>) body);
            }

            // Handle Optional
            if (body instanceof Optional) {
                Optional<?> optional = (Optional<?>) body;
                return optional.map(fieldPermissionService::filterFields);
            }

        } catch (Exception e) {
            logger.error("Error filtering fields: {}", e.getMessage(), e);
        }

        return body;
    }

    /**
     * Filter ApiResponse data
     */
    private com.usermanagement.web.dto.ApiResponse<?> filterApiResponse(
            com.usermanagement.web.dto.ApiResponse<?> apiResponse) {

        Object data = apiResponse.getData();
        if (data == null) {
            return apiResponse;
        }

        Object filteredData;
        if (data instanceof Page) {
            filteredData = filterPage((Page<?>) data);
        } else if (data instanceof Collection) {
            filteredData = fieldPermissionService.filterList((List<?>) data);
        } else if (isDtoObject(data)) {
            filteredData = fieldPermissionService.filterFields(data);
        } else {
            filteredData = data;
        }

        // Create new ApiResponse with filtered data
        com.usermanagement.web.dto.ApiResponse<Object> filtered = new com.usermanagement.web.dto.ApiResponse<>();
        filtered.setSuccess(apiResponse.isSuccess());
        filtered.setMessage(apiResponse.getMessage());
        filtered.setData(filteredData);
        filtered.setTimestamp(apiResponse.getTimestamp());

        return filtered;
    }

    /**
     * Filter Page content
     */
    private <T> Page<T> filterPage(Page<T> page) {
        List<T> filteredContent = fieldPermissionService.filterList(page.getContent());
        return new org.springframework.data.domain.PageImpl<>(
                filteredContent,
                page.getPageable(),
                page.getTotalElements()
        );
    }

    /**
     * Check if object is a DTO that might have @FieldPermission annotations
     */
    private boolean isDtoObject(Object obj) {
        if (obj == null) {
            return false;
        }

        String className = obj.getClass().getName();

        // Check if it's a DTO class
        return className.contains("DTO") ||
               className.contains("Dto") ||
               className.startsWith("com.usermanagement.service.dto");
    }

    /**
     * Extract target user ID from method arguments for ownership check
     */
    private UUID extractTargetUserId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }
        return null;
    }
}
