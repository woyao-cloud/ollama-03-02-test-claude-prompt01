package com.usermanagement.repository;

import com.usermanagement.domain.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户会话仓库接口
 * 提供用户会话数据的CRUD操作和自定义查询
 *
 * @author Database Designer
 * @since 1.0
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * 根据Access Token查找会话
     *
     * @param accessToken Access Token
     * @return 会话对象
     */
    Optional<UserSession> findByAccessToken(String accessToken);

    /**
     * 根据Refresh Token查找会话
     *
     * @param refreshToken Refresh Token
     * @return 会话对象
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);

    /**
     * 根据用户ID查询会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<UserSession> findByUserId(UUID userId);

    /**
     * 根据用户ID分页查询会话
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    Page<UserSession> findByUserId(UUID userId, Pageable pageable);

    /**
     * 查询用户有效的会话
     *
     * @param userId 用户ID
     * @param isValid 是否有效
     * @return 会话列表
     */
    List<UserSession> findByUserIdAndIsValid(UUID userId, Boolean isValid);

    /**
     * 统计用户的有效会话数量
     *
     * @param userId 用户ID
     * @return 会话数量
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user.id = :userId AND s.isValid = true AND s.expiresAt > CURRENT_TIMESTAMP")
    long countValidSessionsByUserId(@Param("userId") UUID userId);

    /**
     * 删除用户的所有会话
     *
     * @param userId 用户ID
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * 删除过期的会话
     *
     * @param expiresAt 过期时间
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :expiresAt")
    void deleteByExpiresAtBefore(@Param("expiresAt") Instant expiresAt);

    /**
     * 使会话失效（登出）
     *
     * @param accessToken Access Token
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isValid = false WHERE s.accessToken = :accessToken")
    void invalidateSession(@Param("accessToken") String accessToken);

    /**
     * 使用户的指定会话失效（保留其他会话）
     *
     * @param userId 用户ID
     * @param accessToken Access Token（保留的会话）
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isValid = false WHERE s.user.id = :userId AND s.accessToken != :accessToken")
    void invalidateOtherSessions(@Param("userId") UUID userId, @Param("accessToken") String accessToken);

    /**
     * 使用户的所有会话失效
     *
     * @param userId 用户ID
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isValid = false WHERE s.user.id = :userId")
    void invalidateAllUserSessions(@Param("userId") UUID userId);

    /**
     * 查询指定客户端IP的会话
     *
     * @param clientIp 客户端IP
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    Page<UserSession> findByClientIp(String clientIp, Pageable pageable);

    /**
     * 查询即将过期的会话
     *
     * @param expiresBefore 过期时间阈值
     * @return 会话列表
     */
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :expiresBefore AND s.isValid = true")
    List<UserSession> findExpiringSessions(@Param("expiresBefore") Instant expiresBefore);

    /**
     * 更新最后访问时间
     *
     * @param accessToken Access Token
     * @param lastAccessedAt 最后访问时间
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :lastAccessedAt WHERE s.accessToken = :accessToken")
    void updateLastAccessedAt(@Param("accessToken") String accessToken, @Param("lastAccessedAt") Instant lastAccessedAt);

    /**
     * 检查Access Token是否已存在
     *
     * @param accessToken Access Token
     * @return 是否存在
     */
    boolean existsByAccessToken(String accessToken);

    /**
     * 检查Refresh Token是否已存在
     *
     * @param refreshToken Refresh Token
     * @return 是否存在
     */
    boolean existsByRefreshToken(String refreshToken);

    /**
     * 查询指定时间范围内创建的会话
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 会话列表
     */
    List<UserSession> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /**
     * 查询指定用户和设备的会话
     *
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     * @return 会话列表
     */
    List<UserSession> findByUserIdAndDeviceInfo(UUID userId, String deviceInfo);
}
