package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Update Role Request DTO
 * Request object for updating an existing role
 *
 * @author Service Team
 * @since 1.0
 */
public class UpdateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Role.DataScope dataScope;

    private Role.RoleStatus status;

    private List<String> permissionCodes;

    // Constructors
    public UpdateRoleRequest() {
    }

    public UpdateRoleRequest(String name, String description, Role.DataScope dataScope, Role.RoleStatus status) {
        this.name = name;
        this.description = description;
        this.dataScope = dataScope;
        this.status = status;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Role.DataScope getDataScope() {
        return dataScope;
    }

    public void setDataScope(Role.DataScope dataScope) {
        this.dataScope = dataScope;
    }

    public Role.RoleStatus getStatus() {
        return status;
    }

    public void setStatus(Role.RoleStatus status) {
        this.status = status;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
