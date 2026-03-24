package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permission Tree DTO
 * Tree structure for hierarchical permission display
 *
 * @author Service Team
 * @since 1.0
 */
public class PermissionTreeDTO {

    private UUID id;
    private String name;
    private String code;
    private Permission.PermissionType type;
    private String resource;
    private String action;
    private String icon;
    private String route;
    private Integer sortOrder;
    private Permission.PermissionStatus status;
    private List<PermissionTreeDTO> children = new ArrayList<>();
    private boolean hasChildren;

    // Constructors
    public PermissionTreeDTO() {
    }

    // Static factory method
    public static PermissionTreeDTO fromEntity(Permission permission) {
        if (permission == null) {
            return null;
        }
        PermissionTreeDTO dto = new PermissionTreeDTO();
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
        dto.setHasChildren(permission.getChildren() != null && !permission.getChildren().isEmpty());
        return dto;
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

    public List<PermissionTreeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<PermissionTreeDTO> children) {
        this.children = children;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    /**
     * Add child node
     */
    public void addChild(PermissionTreeDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
        this.hasChildren = true;
    }
}
