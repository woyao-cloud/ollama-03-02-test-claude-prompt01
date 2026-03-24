package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Permission;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permission DTO
 * Data transfer object for permission information
 *
 * @author Service Team
 * @since 1.0
 */
public class PermissionDTO {

    private UUID id;
    private String name;
    private String code;
    private Permission.PermissionType type;
    private String resource;
    private String action;
    private UUID parentId;
    private String parentName;
    private String icon;
    private String route;
    private Integer sortOrder;
    private Permission.PermissionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public PermissionDTO() {
    }

    public PermissionDTO(UUID id, String name, String code, Permission.PermissionType type,
                         String resource, String action) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.resource = resource;
        this.action = action;
    }

    // Static factory method
    public static PermissionDTO fromEntity(Permission permission) {
        if (permission == null) {
            return null;
        }
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setCode(permission.getCode());
        dto.setType(permission.getType());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        dto.setIcon(permission.getIcon());
        dto.setRoute(permission.getRoute());
        dto.setSortOrder(permission.getSortOrder());
        dto.setStatus(permission.getStatus());
        dto.setCreatedAt(permission.getCreatedAt());
        dto.setUpdatedAt(permission.getUpdatedAt());

        if (permission.getParent() != null) {
            dto.setParentId(permission.getParent().getId());
            dto.setParentName(permission.getParent().getName());
        }

        return dto;
    }

    // Static factory method for list
    public static List<PermissionDTO> fromEntities(List<Permission> permissions) {
        if (permissions == null) {
            return new ArrayList<>();
        }
        return permissions.stream()
                .map(PermissionDTO::fromEntity)
                .toList();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
