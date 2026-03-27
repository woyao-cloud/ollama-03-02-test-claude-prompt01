package com.usermanagement.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Assign Roles Request DTO
 * Request object for assigning roles to a user
 *
 * @author Web Team
 * @since 1.0
 */
public class AssignRolesRequest {

    @NotEmpty(message = "Role IDs cannot be empty")
    private List<UUID> roleIds;

    // Constructors
    public AssignRolesRequest() {}

    public AssignRolesRequest(List<UUID> roleIds) {
        this.roleIds = roleIds;
    }

    // Getters and Setters
    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<UUID> roleIds) {
        this.roleIds = roleIds;
    }
}
