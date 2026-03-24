package com.usermanagement.repository;

import com.usermanagement.domain.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 权限仓库接口
 * 提供权限数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    /**
     * 根据权限编码查找权限
     *
     * @param code 权限编码
     * @return 权限对象
     */
    Optional<Permission> findByCode(String code);

    /**
     * 根据权限类型查询权限
     *
     * @param type 权限类型
     * @return 权限列表
     */
    List<Permission> findByType(Permission.PermissionType type);

    /**
     * 根据父权限ID查询子权限
     *
     * @param parentId 父权限ID
     * @return 权限列表
     */
    List<Permission> findByParentId(UUID parentId);

    /**
     * 根据状态查询权限
     *
     * @param status 权限状态
     * @return 权限列表
     */
    List<Permission> findByStatus(Permission.PermissionStatus status);

    /**
     * 根据类型和状态查询权限
     *
     * @param type 权限类型
     * @param status 权限状态
     * @return 权限列表
     */
    List<Permission> findByTypeAndStatus(Permission.PermissionType type, Permission.PermissionStatus status);

    /**
     * 检查权限编码是否已存在
     *
     * @param code 权限编码
     * @return 是否存在
     */
    boolean existsByCode(String code);

    /**
     * 查询所有根权限（无父权限）
     *
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.parent IS NULL AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Permission> findRootPermissions();

    /**
     * 查询所有菜单权限
     *
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.type = 'MENU' AND p.status = 'ACTIVE' AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Permission> findMenuPermissions();

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p JOIN p.rolePermissions rp WHERE rp.role.id = :roleId AND p.deletedAt IS NULL")
    List<Permission> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * 根据资源类型查询权限
     *
     * @param resource 资源类型
     * @return 权限列表
     */
    List<Permission> findByResource(String resource);

    /**
     * 根据资源类型和操作类型查询权限
     *
     * @param resource 资源类型
     * @param action 操作类型
     * @return 权限对象
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /**
     * 分页查询权限
     *
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    Page<Permission> findByDeletedAtIsNull(Pageable pageable);
}
