package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create Role Request DTO
 * Request object for creating a new role
 *
 * @author Service Team
 * @since 1.0
 */
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Role code is required")
    @Size(min = 2, max = 50, message = "Role code must be between 2 and 50 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Data scope is required")
    private Role.DataScope dataScope = Role.DataScope.ALL;

    private List<String> permissionCodes;

    // Constructors
    public CreateRoleRequest() {
    }

    public CreateRoleRequest(String name, String code, String description, Role.DataScope dataScope) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.dataScope = dataScope;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
