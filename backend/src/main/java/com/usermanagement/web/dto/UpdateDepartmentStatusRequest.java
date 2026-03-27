package com.usermanagement.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Update Department Status Request
 * Request object for updating department status
 *
 * @author Web Team
 * @since 1.0
 */
public class UpdateDepartmentStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    // Constructors
    public UpdateDepartmentStatusRequest() {}

    public UpdateDepartmentStatusRequest(String status) {
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
