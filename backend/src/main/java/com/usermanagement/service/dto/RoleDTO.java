package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Role DTO
 * Data transfer object for role information
 *
 * @author Service Team
 * @since 1.0
 */
public class RoleDTO {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private Role.DataScope dataScope;
    private Role.RoleStatus status;
    private Boolean isSystem;
    private List<PermissionDTO> permissions;
    private List<String> permissionCodes;
    private Integer permissionCount;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public RoleDTO() {
    }

    public RoleDTO(UUID id, String name, String code, String description,
                   Role.DataScope dataScope, Role.RoleStatus status, Boolean isSystem) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.dataScope = dataScope;
        this.status = status;
        this.isSystem = isSystem;
    }

    // Static factory method
    public static RoleDTO fromEntity(Role role) {
        if (role == null) {
            return null;
        }
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setCode(role.getCode());
        dto.setDescription(role.getDescription());
        dto.setDataScope(role.getDataScope());
        dto.setStatus(role.getStatus());
        dto.setIsSystem(role.getIsSystem());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        if (role.getRolePermissions() != null) {
            dto.setPermissionCount(role.getRolePermissions().size());
        }

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

    public Role.RoleStatus getStatus() {
        return status;
    }

    public void setStatus(Role.RoleStatus status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }

    public Integer getPermissionCount() {
        return permissionCount;
    }

    public void setPermissionCount(Integer permissionCount) {
        this.permissionCount = permissionCount;
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
