package com.usermanagement.repository;

import com.usermanagement.domain.entity.Department;
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
 * 部门仓库接口
 * 提供部门数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    /**
     * 根据部门编码查找部门
     *
     * @param code 部门编码
     * @return 部门对象
     */
    Optional<Department> findByCode(String code);

    /**
     * 根据父部门ID查询子部门
     *
     * @param parentId 父部门ID
     * @return 部门列表
     */
    List<Department> findByParentId(UUID parentId);

    /**
     * 根据父部门ID和状态查询子部门
     *
     * @param parentId 父部门ID
     * @param status 部门状态
     * @return 部门列表
     */
    List<Department> findByParentIdAndStatus(UUID parentId, Department.DepartmentStatus status);

    /**
     * 根据层级查询部门
     *
     * @param level 层级
     * @return 部门列表
     */
    List<Department> findByLevel(Integer level);

    /**
     * 根据状态查询部门
     *
     * @param status 部门状态
     * @return 部门列表
     */
    List<Department> findByStatus(Department.DepartmentStatus status);

    /**
     * 根据状态分页查询部门
     *
     * @param status 部门状态
     * @param pageable 分页参数
     * @return 部门分页结果
     */
    Page<Department> findByStatus(Department.DepartmentStatus status, Pageable pageable);

    /**
     * 根据路径前缀查询部门（查询子部门树）
     *
     * @param pathPrefix 路径前缀
     * @return 部门列表
     */
    @Query("SELECT d FROM Department d WHERE d.path LIKE :pathPrefix% AND d.deletedAt IS NULL")
    List<Department> findByPathStartingWith(@Param("pathPrefix") String pathPrefix);

    /**
     * 检查部门编码是否已存在
     *
     * @param code 部门编码
     * @return 是否存在
     */
    boolean existsByCode(String code);

    /**
     * 查询未删除的部门
     *
     * @param pageable 分页参数
     * @return 部门分页结果
     */
    @Query("SELECT d FROM Department d WHERE d.deletedAt IS NULL ORDER BY d.level, d.sortOrder DESC")
    Page<Department> findAllActive(Pageable pageable);

    /**
     * 查询所有未删除的部门
     *
     * @return 部门列表
     */
    @Query("SELECT d FROM Department d WHERE d.deletedAt IS NULL ORDER BY d.level, d.sortOrder DESC")
    List<Department> findAllActive();

    /**
     * 根据父部门ID统计子部门数量
     *
     * @param parentId 父部门ID
     * @return 子部门数量
     */
    @Query("SELECT COUNT(d) FROM Department d WHERE d.parent.id = :parentId AND d.deletedAt IS NULL")
    long countByParentId(@Param("parentId") UUID parentId);

    /**
     * 根据负责人ID查询部门
     *
     * @param managerId 负责人ID
     * @return 部门列表
     */
    List<Department> findByManagerId(UUID managerId);

    /**
     * 查询根部门
     *
     * @return 根部门
     */
    @Query("SELECT d FROM Department d WHERE d.parent IS NULL AND d.deletedAt IS NULL")
    Optional<Department> findRootDepartment();

    /**
     * 根据名称模糊查询部门
     *
     * @param name 部门名称
     * @param pageable 分页参数
     * @return 部门分页结果
     */
    @Query("SELECT d FROM Department d WHERE d.name LIKE %:name% AND d.deletedAt IS NULL")
    Page<Department> findByNameContaining(@Param("name") String name, Pageable pageable);
}
