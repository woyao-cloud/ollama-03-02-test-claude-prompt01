package com.usermanagement.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户角色关联实体的复合主键类
 * 用于支持 UserRole 实体的复合主键
 *
 * @author Database Designer
 * @since 1.0
 */
@Embeddable
public class UserRoleId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID user;

    @Column(name = "role_id", nullable = false)
    private UUID role;

    // 默认构造函数（JPA要求）
    public UserRoleId() {
    }

    // 带参数的构造函数
    public UserRoleId(UUID user, UUID role) {
        this.user = user;
        this.role = role;
    }

    // Getters and Setters
    public UUID getUser() {
        return user;
    }

    public void setUser(UUID user) {
        this.user = user;
    }

    public UUID getRole() {
        return role;
    }

    public void setRole(UUID role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleId)) return false;
        UserRoleId that = (UserRoleId) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }

    @Override
    public String toString() {
        return "UserRoleId{" +
                "userId=" + user +
                ", roleId=" + role +
                '}';
    }
}