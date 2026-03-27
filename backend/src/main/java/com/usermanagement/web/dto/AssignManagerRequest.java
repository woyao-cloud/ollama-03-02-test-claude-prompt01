package com.usermanagement.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Assign Manager Request
 * Request object for assigning manager to department
 *
 * @author Web Team
 * @since 1.0
 */
public class AssignManagerRequest {

    @NotNull(message = "Manager ID is required")
    private UUID managerId;

    // Constructors
    public AssignManagerRequest() {}

    public AssignManagerRequest(UUID managerId) {
        this.managerId = managerId;
    }

    // Getters and Setters
    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }
}
