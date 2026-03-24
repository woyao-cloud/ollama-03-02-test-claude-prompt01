package com.usermanagement.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Update Department Request DTO
 * Request object for updating an existing department
 *
 * @author Service Team
 * @since 1.0
 */
public class UpdateDepartmentRequest {

    @Size(min = 1, max = 100, message = "Department name must be between 1 and 100 characters")
    private String name;

    @Size(min = 2, max = 50, message = "Department code must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Department code can only contain letters, numbers, hyphens, and underscores")
    private String code;

    private UUID parentId;

    private UUID managerId;

    @Min(value = 0, message = "Sort order must be at least 0")
    @Max(value = 9999, message = "Sort order must not exceed 9999")
    private Integer sortOrder;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // Constructors
    public UpdateDepartmentRequest() {}

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

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
