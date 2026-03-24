package com.usermanagement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Update User Status Request DTO
 * Request object for updating user status
 *
 * @author Web Team
 * @since 1.0
 */
public class UpdateUserStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|PENDING|LOCKED)$", message = "Status must be one of: ACTIVE, INACTIVE, PENDING, LOCKED")
    private String status;

    private String reason;

    // Constructors
    public UpdateUserStatusRequest() {}

    public UpdateUserStatusRequest(String status) {
        this.status = status;
    }

    public UpdateUserStatusRequest(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
