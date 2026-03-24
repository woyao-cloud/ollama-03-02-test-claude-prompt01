package com.usermanagement.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门实体类
 * 使用Materialized Path模式存储树形部门结构，支持最多5级层级
 *
 * @author Database Designer
 * @since 1.0
 */
@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_departments_code", columnList = "code", unique = true),
    @Index(name = "idx_departments_path", columnList = "path"),
    @Index(name = "idx_departments_parent", columnList = "parent_id"),
    @Index(name = "idx_departments_level", columnList = "level"),
    @Index(name = "idx_departments_parent_sort", columnList = "parent_id, sort_order DESC")
})
public class Department extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_departments_parent"))
    private Department parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder DESC")
    private List<Department> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", foreignKey = @ForeignKey(name = "fk_departments_manager"))
    private User manager;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    // 部门状态枚举
    public enum DepartmentStatus {
        ACTIVE("激活"),
        INACTIVE("禁用");

        private final String description;

        DepartmentStatus(String description) {
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

    public Department getParent() {
        return parent;
    }

    public void setParent(Department parent) {
        this.parent = parent;
        // 设置父部门时自动计算路径和层级
        if (parent != null) {
            this.level = parent.getLevel() + 1;
            this.path = parent.getPath() + "/" + (this.getId() != null ? this.getId() : "TEMP");
        } else {
            this.level = 1;
            this.path = "/" + (this.getId() != null ? this.getId() : "TEMP");
        }
    }

    public List<Department> getChildren() {
        return children;
    }

    public void setChildren(List<Department> children) {
        this.children = children;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("部门层级必须在1-5之间");
        }
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public DepartmentStatus getStatus() {
        return status;
    }

    public void setStatus(DepartmentStatus status) {
        this.status = status;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    // 业务方法

    /**
     * 是否是根部门
     */
    @Transient
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 是否是叶子部门（无子部门）
     */
    @Transient
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * 获取完整路径（包含当前部门ID）
     */
    @Transient
    public String getFullPath() {
        if (this.getId() == null) {
            return this.path;
        }
        if (parent == null) {
            return "/" + this.getId();
        }
        return parent.getFullPath() + "/" + this.getId();
    }

    /**
     * 获取所有父部门ID列表
     */
    @Transient
    public List<String> getAncestorIds() {
        List<String> ancestors = new ArrayList<>();
        if (path != null && !path.isEmpty()) {
            String[] ids = path.split("/");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    ancestors.add(id);
                }
            }
        }
        return ancestors;
    }

    /**
     * 获取父部门数量（层级深度）
     */
    @Transient
    public int getDepth() {
        return level - 1;
    }

    /**
     * 添加子部门
     */
    public void addChild(Department child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * 移除子部门
     */
    public void removeChild(Department child) {
        children.remove(child);
        child.setParent(null);
    }

    /**
     * 检查是否包含指定部门（包括自身）
     */
    public boolean contains(Department department) {
        if (department == null) {
            return false;
        }
        if (this.equals(department)) {
            return true;
        }
        return department.getPath() != null && department.getPath().startsWith(this.getPath());
    }

    @Override
    public String toString() {
        return "Department{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", level=" + level +
                ", path='" + path + '\'' +
                '}';
    }
}
