package com.usermanagement.repository;

import com.usermanagement.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 审计日志仓库接口
 * 提供审计日志数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * 根据用户ID分页查询审计日志
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * 根据用户名分页查询审计日志
     *
     * @param username 用户名
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByUsernameContaining(String username, Pageable pageable);

    /**
     * 根据操作类型分页查询审计日志
     *
     * @param operation 操作类型
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByOperation(AuditLog.OperationType operation, Pageable pageable);

    /**
     * 根据资源类型分页查询审计日志
     *
     * @param resourceType 资源类型
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);

    /**
     * 根据资源类型和资源ID查询审计日志
     *
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId, Pageable pageable);

    /**
     * 根据操作结果分页查询审计日志
     *
     * @param success 操作是否成功
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findBySuccess(Boolean success, Pageable pageable);

    /**
     * 根据时间范围分页查询审计日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByCreatedAtBetween(Instant startTime, Instant endTime, Pageable pageable);

    /**
     * 根据客户端IP分页查询审计日志
     *
     * @param clientIp 客户端IP
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByClientIp(String clientIp, Pageable pageable);

    /**
     * 综合查询审计日志
     *
     * @param userId 用户ID
     * @param operation 操作类型
     * @param resourceType 资源类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param success 操作是否成功
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId IS NULL OR al.user.id = :userId) AND " +
           "(:operation IS NULL OR al.operation = :operation) AND " +
           "(:resourceType IS NULL OR al.resourceType = :resourceType) AND " +
           "(:startTime IS NULL OR al.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR al.createdAt <= :endTime) AND " +
           "(:success IS NULL OR al.success = :success)")
    Page<AuditLog> findByConditions(
            @Param("userId") UUID userId,
            @Param("operation") AuditLog.OperationType operation,
            @Param("resourceType") String resourceType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("success") Boolean success,
            Pageable pageable);

    /**
     * 查询最近的审计日志
     *
     * @param limit 限制数量
     * @return 审计日志列表
     */
    @Query("SELECT al FROM AuditLog al ORDER BY al.createdAt DESC")
    List<AuditLog> findRecent(Pageable pageable);

    /**
     * 根据会话ID查询审计日志
     *
     * @param sessionId 会话ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findBySessionId(String sessionId, Pageable pageable);

    /**
     * 统计指定时间范围内的日志数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.createdAt BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 统计指定操作类型的日志数量
     *
     * @param operation 操作类型
     * @return 日志数量
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.operation = :operation")
    long countByOperation(@Param("operation") AuditLog.OperationType operation);

    /**
     * 统计失败的登录尝试
     *
     * @param startTime 开始时间
     * @param clientIp 客户端IP
     * @return 失败次数
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.operation = 'LOGIN' AND al.success = false AND al.createdAt >= :startTime AND al.clientIp = :clientIp")
    long countFailedLoginAttempts(@Param("startTime") Instant startTime, @Param("clientIp") String clientIp);
}
