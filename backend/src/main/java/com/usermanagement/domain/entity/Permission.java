package com.usermanagement.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限实体类
 * 支持四级权限模型：菜单、操作、字段、数据
 *
 * @author Database Designer
 * @since 1.0
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_code", columnList = "code", unique = true),
    @Index(name = "idx_permissions_type", columnList = "type"),
    @Index(name = "idx_permissions_resource", columnList = "resource"),
    @Index(name = "idx_permissions_parent", columnList = "parent_id"),
    @Index(name = "idx_permissions_status", columnList = "status")
})
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PermissionType type = PermissionType.ACTION;

    @Column(name = "resource", nullable = false, length = 50)
    private String resource;

    @Column(name = "action", length = 50)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_permissions_parent"))
    private Permission parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<Permission> children = new ArrayList<>();

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "route", length = 200)
    private String route;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PermissionStatus status = PermissionStatus.ACTIVE;

    // 权限类型枚举
    public enum PermissionType {
        MENU("菜单权限", "控制导航菜单显示"),
        ACTION("操作权限", "控制页面按钮操作"),
        FIELD("字段权限", "控制字段显示/编辑"),
        DATA("数据权限", "控制数据访问范围");

        private final String name;
        private final String description;

        PermissionType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    // 权限状态枚举
    public enum PermissionStatus {
        ACTIVE("激活"),
        INACTIVE("禁用");

        private final String description;

        PermissionStatus(String description) {
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

    public PermissionType getType() {
        return type;
    }

    public void setType(PermissionType type) {
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

    public Permission getParent() {
        return parent;
    }

    public void setParent(Permission parent) {
        this.parent = parent;
    }

    public List<Permission> getChildren() {
        return children;
    }

    public void setChildren(List<Permission> children) {
        this.children = children;
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

    public PermissionStatus getStatus() {
        return status;
    }

    public void setStatus(PermissionStatus status) {
        this.status = status;
    }

    // 业务方法

    /**
     * 是否是菜单权限
     */
    @Transient
    public boolean isMenu() {
        return type == PermissionType.MENU;
    }

    /**
     * 是否是操作权限
     */
    @Transient
    public boolean isAction() {
        return type == PermissionType.ACTION;
    }

    /**
     * 是否是根权限（无父权限）
     */
    @Transient
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 是否是叶子权限（无子权限）
     */
    @Transient
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * 获取权限标识符（resource:action格式）
     */
    @Transient
    public String getIdentifier() {
        if (action != null && !action.isEmpty()) {
            return resource + ":" + action;
        }
        return resource;
    }

    /**
     * 添加子权限
     */
    public void addChild(Permission child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * 判断是否激活
     */
    @Transient
    public boolean isActive() {
        return status == PermissionStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", type=" + type +
                ", resource='" + resource + '\'' +
                '}';
    }
}
