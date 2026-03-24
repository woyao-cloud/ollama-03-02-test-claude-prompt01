package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Department;

import java.util.UUID;

/**
 * Department Query Request DTO
 * Request object for querying departments with filters and pagination
 *
 * @author Service Team
 * @since 1.0
 */
public class DepartmentQueryRequest {

    private String keyword;
    private UUID parentId;
    private Department.DepartmentStatus status;
    private Integer level;
    private int page = 0;
    private int size = 20;
    private String sortBy = "sortOrder";
    private String sortDirection = "DESC";

    // Normalize query parameters
    public void normalize() {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
        if (!sortBy.matches("^(name|code|level|sortOrder|createdAt)$")) {
            sortBy = "sortOrder";
        }
        if (!sortDirection.matches("^(ASC|DESC)$")) {
            sortDirection = "DESC";
        }
    }

    // Constructors
    public DepartmentQueryRequest() {}

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Department.DepartmentStatus getStatus() {
        return status;
    }

    public void setStatus(Department.DepartmentStatus status) {
        this.status = status;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
