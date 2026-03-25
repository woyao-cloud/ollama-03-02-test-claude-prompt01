package com.usermanagement.repository;

import com.usermanagement.domain.entity.OAuth2Connection;
import com.usermanagement.domain.entity.OAuth2Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth2 Connection Repository
 * Provides CRUD operations for OAuth2 connections
 *
 * @author Repository Team
 * @since 1.0
 */
@Repository
public interface OAuth2ConnectionRepository extends JpaRepository<OAuth2Connection, UUID> {

    /**
     * Find connections by user ID
     *
     * @param userId user ID
     * @return list of OAuth2 connections
     */
    List<OAuth2Connection> findByUserId(UUID userId);

    /**
     * Find connection by user ID and provider
     *
     * @param userId user ID
     * @param provider OAuth2 provider
     * @return optional OAuth2 connection
     */
    Optional<OAuth2Connection> findByUserIdAndProvider(UUID userId, OAuth2Provider provider);

    /**
     * Find connection by provider and provider user ID
     *
     * @param provider OAuth2 provider
     * @param providerUserId provider user ID
     * @return optional OAuth2 connection
     */
    Optional<OAuth2Connection> findByProviderAndProviderUserId(OAuth2Provider provider, String providerUserId);

    /**
     * Find primary connection by user ID
     *
     * @param userId user ID
     * @return optional primary OAuth2 connection
     */
    Optional<OAuth2Connection> findByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * Count connections by user ID
     *
     * @param userId user ID
     * @return number of connections
     */
    long countByUserId(UUID userId);

    /**
     * Check if connection exists for user and provider
     *
     * @param userId user ID
     * @param provider OAuth2 provider
     * @return true if exists
     */
    boolean existsByUserIdAndProvider(UUID userId, OAuth2Provider provider);

    /**
     * Check if provider user ID exists for provider
     *
     * @param provider OAuth2 provider
     * @param providerUserId provider user ID
     * @return true if exists
     */
    boolean existsByProviderAndProviderUserId(OAuth2Provider provider, String providerUserId);

    /**
     * Delete connections by user ID
     *
     * @param userId user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete connection by user ID and provider
     *
     * @param userId user ID
     * @param provider OAuth2 provider
     */
    void deleteByUserIdAndProvider(UUID userId, OAuth2Provider provider);

    /**
     * Find all connections by provider
     *
     * @param provider OAuth2 provider
     * @return list of connections
     */
    List<OAuth2Connection> findByProvider(OAuth2Provider provider);

    /**
     * Find connection by email and provider
     *
     * @param email email address
     * @param provider OAuth2 provider
     * @return optional OAuth2 connection
     */
    Optional<OAuth2Connection> findByEmailAndProvider(String email, OAuth2Provider provider);

    /**
     * Get active connections count by provider
     *
     * @param provider OAuth2 provider
     * @return count of active connections
     */
    @Query("SELECT COUNT(c) FROM OAuth2Connection c WHERE c.provider = :provider AND c.deletedAt IS NULL")
    long countByProvider(@Param("provider") OAuth2Provider provider);
}
