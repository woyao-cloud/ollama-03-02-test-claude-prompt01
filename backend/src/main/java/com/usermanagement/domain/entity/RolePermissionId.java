package com.usermanagement.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * 角色权限关联实体的复合主键类
 * 用于支持 RolePermission 实体的复合主键
 *
 * @author Database Designer
 * @since 1.0
 */
@Embeddable
public class RolePermissionId implements Serializable {

    @Column(name = "role_id", nullable = false)
    private UUID role;

    @Column(name = "permission_id", nullable = false)
    private UUID permission;

    // 默认构造函数（JPA要求）
    public RolePermissionId() {
    }

    // 带参数的构造函数
    public RolePermissionId(UUID role, UUID permission) {
        this.role = role;
        this.permission = permission;
    }

    // Getters and Setters
    public UUID getRole() {
        return role;
    }

    public void setRole(UUID role) {
        this.role = role;
    }

    public UUID getPermission() {
        return permission;
    }

    public void setPermission(UUID permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePermissionId)) return false;
        RolePermissionId that = (RolePermissionId) o;
        return Objects.equals(role, that.role) &&
                Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, permission);
    }

    @Override
    public String toString() {
        return "RolePermissionId{" +
                "role=" + role +
                ", permission=" + permission +
                '}';
    }
}