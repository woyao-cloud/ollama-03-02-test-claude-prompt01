package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Department;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Department DTO
 * Data transfer object for department information
 *
 * @author Service Team
 * @since 1.0
 */
public class DepartmentDTO {

    private UUID id;
    private String name;
    private String code;
    private UUID parentId;
    private String parentName;
    private UUID managerId;
    private String managerName;
    private Integer level;
    private String path;
    private Integer sortOrder;
    private String description;
    private Department.DepartmentStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<DepartmentDTO> children;
    private Long userCount;

    // Constructors
    public DepartmentDTO() {}

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

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
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

    public Department.DepartmentStatus getStatus() {
        return status;
    }

    public void setStatus(Department.DepartmentStatus status) {
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

    public List<DepartmentDTO> getChildren() {
        return children;
    }

    public void setChildren(List<DepartmentDTO> children) {
        this.children = children;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    /**
     * Get status display text
     */
    public String getStatusDisplay() {
        if (status == null) return "";
        return status.getDescription();
    }

    /**
     * Check if this is a root department
     */
    public boolean isRoot() {
        return parentId == null;
    }
}
