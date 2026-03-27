package com.usermanagement.web.dto;

import java.util.UUID;

/**
 * Move Department Request
 * Request object for moving department to new parent
 *
 * @author Web Team
 * @since 1.0
 */
public class MoveDepartmentRequest {

    private UUID parentId;

    // Constructors
    public MoveDepartmentRequest() {}

    public MoveDepartmentRequest(UUID parentId) {
        this.parentId = parentId;
    }

    // Getters and Setters
    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }
}
