package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.User;
import com.usermanagement.security.fieldpermission.FieldPermission;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User DTO
 * Data transfer object for user information
 *
 * @author Service Team
 * @since 1.0
 */
public class UserDTO {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;

    @FieldPermission(resource = "user", field = "phone", mask = FieldPermission.MaskType.PARTIAL)
    private String phone;

    private String avatarUrl;
    private UUID departmentId;
    private String departmentName;
    private User.UserStatus status;
    private Boolean emailVerified;

    @FieldPermission(resource = "user", field = "lastLoginAt", mask = FieldPermission.MaskType.NULL)
    private Instant lastLoginAt;

    private Instant createdAt;
    private Instant updatedAt;

    @FieldPermission(resource = "user", field = "roles", mask = FieldPermission.MaskType.EMPTY)
    private List<RoleInfo> roles;

    // Inner class for role information
    public static class RoleInfo {
        private UUID id;
        private String name;
        private String code;

        public RoleInfo() {}

        public RoleInfo(UUID id, String name, String code) {
            this.id = id;
            this.name = name;
            this.code = code;
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
    }

    // Constructors
    public UserDTO() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public User.UserStatus getStatus() {
        return status;
    }

    public void setStatus(User.UserStatus status) {
        this.status = status;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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

    public List<RoleInfo> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleInfo> roles) {
        this.roles = roles;
    }

    /**
     * Get status display text
     */
    public String getStatusDisplay() {
        if (status == null) return "";
        return status.getDescription();
    }
}
