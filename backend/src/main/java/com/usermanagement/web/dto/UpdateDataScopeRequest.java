package com.usermanagement.web.dto;

import com.usermanagement.domain.entity.Role;

import jakarta.validation.constraints.NotNull;

/**
 * Update Data Scope Request DTO
 * Request object for updating role data scope
 *
 * @author Web Team
 * @since 1.0
 */
public class UpdateDataScopeRequest {

    @NotNull(message = "Data scope is required")
    private Role.DataScope dataScope;

    // Constructors
    public UpdateDataScopeRequest() {
    }

    public UpdateDataScopeRequest(Role.DataScope dataScope) {
        this.dataScope = dataScope;
    }

    // Getters and Setters
    public Role.DataScope getDataScope() {
        return dataScope;
    }

    public void setDataScope(Role.DataScope dataScope) {
        this.dataScope = dataScope;
    }
}
