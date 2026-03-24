package com.usermanagement.repository;

import com.usermanagement.domain.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Password History Repository
 *
 * @author Repository Team
 * @since 1.0
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    /**
     * Find recent password history for a user
     *
     * @param userId user ID
     * @param limit maximum number of records
     * @return list of password history
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.userId = :userId ORDER BY ph.createdAt DESC LIMIT :limit")
    List<PasswordHistory> findRecentByUserId(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * Count password history for a user
     *
     * @param userId user ID
     * @return count
     */
    long countByUserId(UUID userId);

    /**
     * Delete old password history for a user
     *
     * @param userId user ID
     */
    void deleteByUserId(UUID userId);
}
