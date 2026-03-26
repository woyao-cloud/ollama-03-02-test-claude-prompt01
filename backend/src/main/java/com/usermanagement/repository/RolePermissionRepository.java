package com.usermanagement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.usermanagement.domain.entity.RolePermission;
import com.usermanagement.domain.entity.RolePermissionId;

/**
 * 角色权限关联仓库接口
 * 提供角色权限关联数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    /**
     * 根据角色ID查询角色权限关联
     *
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    @Query(value = "SELECT rp.* FROM role_permissions rp WHERE rp.role_id = :roleId", nativeQuery = true)
    List<RolePermission> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * 根据权限ID查询角色权限关联
     *
     * @param permissionId 权限ID
     * @return 角色权限关联列表
     */
    @Query(value = "SELECT rp.* FROM role_permissions rp WHERE rp.permission_id = :permissionId", nativeQuery = true)
    List<RolePermission> findByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * 删除角色的所有权限关联
     *
     * @param roleId 角色ID
     */
    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId", nativeQuery = true)
    void deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 删除权限的所有角色关联
     *
     * @param permissionId 权限ID
     */
    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    void deleteByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * 检查角色是否已有指定权限
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 是否存在
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId)", nativeQuery = true)
    boolean existsByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    /**
     * 统计角色的权限数量
     *
     * @param roleId 角色ID
     * @return 权限数量
     */
    @Query(value = "SELECT COUNT(*) FROM role_permissions WHERE role_id = :roleId", nativeQuery = true)
    long countByRoleId(@Param("roleId") UUID roleId);

    /**
     * 统计权限的角色数量
     *
     * @param permissionId 权限ID
     * @return 角色数量
     */
    @Query(value = "SELECT COUNT(*) FROM role_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    long countByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * 删除指定的角色权限关联
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     */
    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId", nativeQuery = true)
    void deleteByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    /**
     * 查询拥有指定权限的所有角色ID
     *
     * @param permissionId 权限ID
     * @return 角色ID列表
     */
    @Query(value = "SELECT rp.role_id FROM role_permissions rp WHERE rp.permission_id = :permissionId", nativeQuery = true)
    List<UUID> findRoleIdsByPermissionId(@Param("permissionId") UUID permissionId);
}
