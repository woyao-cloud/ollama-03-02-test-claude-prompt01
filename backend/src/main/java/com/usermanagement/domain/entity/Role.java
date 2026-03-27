package com.usermanagement.domain.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * 定义系统角色，包含数据权限范围配置
 *
 * @author Database Designer
 * @since 1.0
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_roles_name", columnList = "name", unique = true),
    @Index(name = "idx_roles_code", columnList = "code", unique = true),
    @Index(name = "idx_roles_status", columnList = "status")
})
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_scope", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DataScope dataScope = DataScope.ALL;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoleStatus status = RoleStatus.ACTIVE;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    // 数据权限范围枚举
    public enum DataScope {
        ALL("全部数据"),
        DEPT("本部门及子部门"),
        SELF("仅本人数据"),
        CUSTOM("自定义范围");

        private final String description;

        DataScope(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 角色状态枚举
    public enum RoleStatus {
        ACTIVE("激活"),
        INACTIVE("禁用");

        private final String description;

        RoleStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public void setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void setStatus(RoleStatus status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Set<RolePermission> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(Set<RolePermission> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    // 业务方法

    /**
     * 添加权限
     */
    public void addPermission(Permission permission) {
        RolePermission rp = new RolePermission();
        rp.setRole(this);
        rp.setPermission(permission);
        this.rolePermissions.add(rp);
    }

    /**
     * 移除权限
     */
    public void removePermission(Permission permission) {
        this.rolePermissions.removeIf(rp -> rp.getPermission().equals(permission));
    }

    /**
     * 判断是否拥有指定权限
     */
    public boolean hasPermission(Permission permission) {
        return this.rolePermissions.stream()
                .anyMatch(rp -> rp.getPermission().equals(permission));
    }

    /**
     * 判断是否拥有指定权限编码
     */
    public boolean hasPermissionCode(String permissionCode) {
        return this.rolePermissions.stream()
                .anyMatch(rp -> rp.getPermission().getCode().equals(permissionCode));
    }

    /**
     * 是否系统预设角色（不可删除）
     */
    @Transient
    public boolean isSystem() {
        return isSystem != null && isSystem;
    }

    /**
     * 判断是否激活
     */
    @Transient
    public boolean isActive() {
        return status == RoleStatus.ACTIVE && !isDeleted();
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", dataScope=" + dataScope +
                '}';
    }
}
