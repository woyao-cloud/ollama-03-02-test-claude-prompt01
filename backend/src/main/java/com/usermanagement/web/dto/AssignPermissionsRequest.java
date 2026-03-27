package com.usermanagement.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Assign Permissions Request DTO
 * Request object for assigning permissions to a role
 *
 * @author Web Team
 * @since 1.0
 */
public class AssignPermissionsRequest {

    @NotEmpty(message = "Permission IDs are required")
    private List<UUID> permissionIds;

    // Constructors
    public AssignPermissionsRequest() {
    }

    public AssignPermissionsRequest(List<UUID> permissionIds) {
        this.permissionIds = permissionIds;
    }

    // Getters and Setters
    public List<UUID> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<UUID> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
