package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Update Permission Request DTO
 * Request object for updating an existing permission
 *
 * @author Service Team
 * @since 1.0
 */
public class UpdatePermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(min = 2, max = 100, message = "Permission name must be between 2 and 100 characters")
    private String name;

    private Permission.PermissionType type;

    @Size(max = 50, message = "Resource must not exceed 50 characters")
    private String resource;

    @Size(max = 50, message = "Action must not exceed 50 characters")
    private String action;

    private UUID parentId;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @Size(max = 200, message = "Route must not exceed 200 characters")
    private String route;

    private Integer sortOrder;

    private Permission.PermissionStatus status;

    // Constructors
    public UpdatePermissionRequest() {
    }

    public UpdatePermissionRequest(String name, String resource, String action) {
        this.name = name;
        this.resource = resource;
        this.action = action;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Permission.PermissionType getType() {
        return type;
    }

    public void setType(Permission.PermissionType type) {
        this.type = type;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Permission.PermissionStatus getStatus() {
        return status;
    }

    public void setStatus(Permission.PermissionStatus status) {
        this.status = status;
    }
}
