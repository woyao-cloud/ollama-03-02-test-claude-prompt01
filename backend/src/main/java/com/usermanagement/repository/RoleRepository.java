package com.usermanagement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.usermanagement.domain.entity.Role;

/**
 * 角色仓库接口
 * 提供角色数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * 根据角色编码查找角色
     *
     * @param code 角色编码
     * @return 角色对象
     */
    Optional<Role> findByCode(String code);

    /**
     * 根据状态查询角色
     *
     * @param status 角色状态
     * @return 角色列表
     */
    List<Role> findByStatus(Role.RoleStatus status);

    /**
     * 根据状态分页查询角色
     *
     * @param status 角色状态
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByStatus(Role.RoleStatus status, Pageable pageable);

    /**
     * 检查角色编码是否已存在
     *
     * @param code 角色编码
     * @return 是否存在
     */
    boolean existsByCode(String code);

    /**
     * 检查角色名称是否已存在
     *
     * @param name 角色名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 查询未删除的角色
     *
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    @Query("SELECT r FROM Role r WHERE r.deletedAt IS NULL")
    Page<Role> findAllActive(Pageable pageable);

    /**
     * 查询所有未删除的角色
     *
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.deletedAt IS NULL")
    List<Role> findAllActive();

    /**
     * 根据数据权限范围查询角色
     *
     * @param dataScope 数据权限范围
     * @return 角色列表
     */
    List<Role> findByDataScope(Role.DataScope dataScope);

    /**
     * 查询系统预设角色
     *
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.isSystem = true AND r.deletedAt IS NULL")
    List<Role> findSystemRoles();

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.userRoles ur WHERE ur.user.id = :userId AND r.deletedAt IS NULL")
    List<Role> findByUserId(@Param("userId") UUID userId);
}
