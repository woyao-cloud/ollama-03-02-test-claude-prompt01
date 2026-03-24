package com.usermanagement.service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Session Service Interface
 * Manages user sessions in Redis
 *
 * @author Service Team
 * @since 1.0
 */
public interface SessionService {

    // Redis key prefixes
    String SESSION_PREFIX = "session:";
    String BLACKLIST_PREFIX = "jwt:blacklist:";
    String FAILED_LOGIN_PREFIX = "login:failed:";
    String LOCKED_PREFIX = "login:locked:";

    /**
     * Create a new session
     *
     * @param userId user ID
     * @param sessionId session ID
     * @param accessTokenJti access token JTI
     * @param refreshTokenJti refresh token JTI
     * @param ttlSeconds session TTL in seconds
     */
    void createSession(UUID userId, String sessionId, String accessTokenJti, String refreshTokenJti, long ttlSeconds);

    /**
     * Get session data
     *
     * @param userId user ID
     * @param sessionId session ID
     * @return session data
     */
    SessionData getSession(UUID userId, String sessionId);

    /**
     * Delete session
     *
     * @param userId user ID
     * @param sessionId session ID
     */
    void deleteSession(UUID userId, String sessionId);

    /**
     * Add token to blacklist
     *
     * @param jti token JTI
     * @param expiration token expiration date
     */
    void addToBlacklist(String jti, Date expiration);

    /**
     * Check if token is blacklisted
     *
     * @param jti token JTI
     * @return true if blacklisted
     */
    boolean isBlacklisted(String jti);

    /**
     * Increment failed login attempts
     *
     * @param email user email
     * @return current failed attempts count
     */
    int incrementFailedLogin(String email);

    /**
     * Get failed login attempts
     *
     * @param email user email
     * @return failed attempts count
     */
    int getFailedLoginCount(String email);

    /**
     * Reset failed login attempts
     *
     * @param email user email
     */
    void resetFailedLogin(String email);

    /**
     * Lock account
     *
     * @param email user email
     * @param minutes lock duration in minutes
     */
    void lockAccount(String email, int minutes);

    /**
     * Check if account is locked
     *
     * @param email user email
     * @return true if locked
     */
    boolean isLocked(String email);

    /**
     * Session data class
     */
    class SessionData {
        private String accessTokenJti;
        private String refreshTokenJti;
        private String createdAt;
        private String expiresAt;

        public SessionData() {}

        public SessionData(String accessTokenJti, String refreshTokenJti, String createdAt, String expiresAt) {
            this.accessTokenJti = accessTokenJti;
            this.refreshTokenJti = refreshTokenJti;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public String getAccessTokenJti() {
            return accessTokenJti;
        }

        public void setAccessTokenJti(String accessTokenJti) {
            this.accessTokenJti = accessTokenJti;
        }

        public String getRefreshTokenJti() {
            return refreshTokenJti;
        }

        public void setRefreshTokenJti(String refreshTokenJti) {
            this.refreshTokenJti = refreshTokenJti;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(String expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
