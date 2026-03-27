package com.usermanagement.security.fieldpermission;

import java.util.*;

/**
 * Field Filter Context
 * Thread-local context for field-level permission filtering
 *
 * @author Security Team
 * @since 1.0
 */
public class FieldFilterContext {

    private static final ThreadLocal<FieldFilterContext> CONTEXT = new ThreadLocal<>();

    private final Set<String> allowedFields = new HashSet<>();
    private final Set<String> deniedFields = new HashSet<>();
    private final Set<String> allowedResources = new HashSet<>();
    private boolean filterEnabled = true;
    private UUID targetUserId;

    private FieldFilterContext() {}

    /**
     * Get current context (creates if not exists)
     */
    public static FieldFilterContext current() {
        FieldFilterContext ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new FieldFilterContext();
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

    // Configuration methods

    public FieldFilterContext allowField(String resource, String field) {
        this.allowedFields.add(resource + "." + field);
        return this;
    }

    public FieldFilterContext allowFields(String resource, Collection<String> fields) {
        for (String field : fields) {
            allowField(resource, field);
        }
        return this;
    }

    public FieldFilterContext denyField(String resource, String field) {
        this.deniedFields.add(resource + "." + field);
        return this;
    }

    public FieldFilterContext allowResource(String resource) {
        this.allowedResources.add(resource);
        return this;
    }

    public FieldFilterContext disableFilter() {
        this.filterEnabled = false;
        return this;
    }

    public FieldFilterContext enableFilter() {
        this.filterEnabled = true;
        return this;
    }

    public FieldFilterContext withTargetUserId(UUID userId) {
        this.targetUserId = userId;
        return this;
    }

    // Query methods

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public boolean isFieldAllowed(String resource, String field) {
        if (!filterEnabled) {
            return true;
        }

        String fullField = resource + "." + field;

        // Check denied fields first (deny takes precedence)
        if (deniedFields.contains(fullField)) {
            return false;
        }

        // Check explicitly allowed fields
        if (allowedFields.contains(fullField)) {
            return true;
        }

        // Check if entire resource is allowed
        if (allowedResources.contains(resource)) {
            return true;
        }

        // Default: deny if we have explicit allowed fields, allow otherwise
        return allowedFields.isEmpty();
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public Set<String> getAllowedFields() {
        return Collections.unmodifiableSet(allowedFields);
    }

    public Set<String> getDeniedFields() {
        return Collections.unmodifiableSet(deniedFields);
    }

    /**
     * Check if user is accessing their own data
     */
    public boolean isSelfAccess(UUID currentUserId) {
        return targetUserId != null && targetUserId.equals(currentUserId);
    }

    @Override
    public String toString() {
        return "FieldFilterContext{" +
                "filterEnabled=" + filterEnabled +
                ", allowedFields=" + allowedFields.size() +
                ", deniedFields=" + deniedFields.size() +
                ", targetUserId=" + targetUserId +
                '}';
    }
}
