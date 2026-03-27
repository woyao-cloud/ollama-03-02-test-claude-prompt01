package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.Role;

import jakarta.validation.constraints.Min;

/**
 * Role Query Request DTO
 * Request object for querying roles with pagination and filters
 *
 * @author Service Team
 * @since 1.0
 */
public class RoleQueryRequest {

    private String keyword;
    private Role.DataScope dataScope;
    private Role.RoleStatus status;
    private Boolean isSystem;

    @Min(value = 0, message = "Page number must be non-negative")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    private int size = 20;

    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    // Constructors
    public RoleQueryRequest() {
    }

    public RoleQueryRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
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
