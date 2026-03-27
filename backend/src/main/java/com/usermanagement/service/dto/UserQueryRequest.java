package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.User;

import java.util.UUID;

/**
 * User Query Request DTO
 * Request object for querying users with filters and pagination
 *
 * @author Service Team
 * @since 1.0
 */
public class UserQueryRequest {

    // Search criteria
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private User.UserStatus status;
    private UUID departmentId;
    private UUID roleId;

    // Pagination
    private Integer page = 0;
    private Integer size = 20;

    // Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    // Constructors
    public UserQueryRequest() {}

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public User.UserStatus getStatus() {
        return status;
    }

    public void setStatus(User.UserStatus status) {
        this.status = status;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page != null ? page : 0;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size != null ? size : 20;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy != null ? sortBy : "createdAt";
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection != null ? sortDirection : "DESC";
    }

    /**
     * Check if request has any search criteria
     */
    public boolean hasCriteria() {
        return email != null && !email.isEmpty()
                || firstName != null && !firstName.isEmpty()
                || lastName != null && !lastName.isEmpty()
                || phone != null && !phone.isEmpty()
                || status != null
                || departmentId != null
                || roleId != null;
    }

    /**
     * Validate and normalize the request
     */
    public void normalize() {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        // Limit max page size for performance
        if (size > 100) {
            size = 100;
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null || sortDirection.isEmpty()) {
            sortDirection = "DESC";
        }
    }
}
