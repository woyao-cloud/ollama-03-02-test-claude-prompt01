package com.usermanagement.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.usermanagement.domain.entity.User;

/**
 * 用户仓库接口
 * 提供用户数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱地址
     * @return 用户对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱地址
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据状态分页查询用户
     *
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    /**
     * 根据部门ID查询用户
     *
     * @param departmentId 部门ID
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByDepartmentId(UUID departmentId, Pageable pageable);

    /**
     * 查询未删除的用户
     *
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllActive(Pageable pageable);

    /**
     * 根据邮箱查找未删除的用户
     *
     * @param email 邮箱地址
     * @return 用户对象
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);

    /**
     * 统计活跃用户数量
     *
     * @return 用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    long countActiveUsers();

    /**
     * 根据角色ID查询用户
     *
     * @param roleId 角色ID
     * @param pageable 分页参数
     * @return 用户分页结果
     */
        @Query(value = "SELECT DISTINCT u FROM User u JOIN u.userRoles ur WHERE ur.role.id = :roleId AND u.deletedAt IS NULL",
            countQuery = "SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles ur WHERE ur.role.id = :roleId AND u.deletedAt IS NULL")
        Page<User> findByRoleId(@Param("roleId") UUID roleId, Pageable pageable);

    /**
     * 统计部门下的用户数量
     *
     * @param departmentId 部门ID
     * @return 用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId AND u.deletedAt IS NULL")
    long countByDepartmentId(@Param("departmentId") UUID departmentId);
}
