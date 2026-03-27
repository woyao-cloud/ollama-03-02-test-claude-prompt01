package com.usermanagement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.usermanagement.domain.entity.UserRole;
import com.usermanagement.domain.entity.UserRoleId;

/**
 * 用户角色关联仓库接口
 * 提供用户角色关联数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    /**
     * 根据用户ID查询用户角色关联
     *
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.id.user = :userId")
    List<UserRole> findByUserId(@Param("userId") UUID userId);

    /**
     * 根据角色ID查询用户角色关联
     *
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.id.role = :roleId")
    List<UserRole> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * 删除用户的所有角色关联
     *
     * @param userId 用户ID
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.id.user = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * 删除角色的所有用户关联
     *
     * @param roleId 角色ID
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.id.role = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 检查用户是否已有指定角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.id.user = :userId AND ur.id.role = :roleId")
    boolean existsByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * 统计用户的角色数量
     *
     * @param userId 用户ID
     * @return 角色数量
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.id.user = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * 统计角色的用户数量
     *
     * @param roleId 角色ID
     * @return 用户数量
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.id.role = :roleId")
    long countByRoleId(@Param("roleId") UUID roleId);

    /**
     * 删除指定的用户角色关联
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.id.user = :userId AND ur.id.role = :roleId")
    void deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
