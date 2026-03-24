package com.usermanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Session Service Implementation
 * Manages user sessions, token blacklist, and login attempt tracking in Redis
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class SessionServiceImpl implements SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    // Default TTL values
    private static final long FAILED_LOGIN_TTL_MINUTES = 30;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public SessionServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              RedisTemplate<String, String> stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void createSession(UUID userId, String sessionId, String accessTokenJti, String refreshTokenJti, long ttlSeconds) {
        String key = buildSessionKey(userId, sessionId);
        SessionData sessionData = new SessionData(
                accessTokenJti,
                refreshTokenJti,
                Instant.now().toString(),
                Instant.now().plusSeconds(ttlSeconds).toString()
        );

        redisTemplate.opsForValue().set(key, sessionData, ttlSeconds, TimeUnit.SECONDS);
        logger.debug("Created session: {} for user: {}", sessionId, userId);
    }

    @Override
    public SessionData getSession(UUID userId, String sessionId) {
        String key = buildSessionKey(userId, sessionId);
        Object data = redisTemplate.opsForValue().get(key);

        if (data instanceof SessionData) {
            return (SessionData) data;
        }
        return null;
    }

    @Override
    public void deleteSession(UUID userId, String sessionId) {
        String key = buildSessionKey(userId, sessionId);
        redisTemplate.delete(key);
        logger.debug("Deleted session: {} for user: {}", sessionId, userId);
    }

    @Override
    public void addToBlacklist(String jti, Date expiration) {
        if (jti == null) {
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        long ttlSeconds = calculateTtl(expiration);

        stringRedisTemplate.opsForValue().set(key, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
        logger.debug("Added token to blacklist: {} (TTL: {} seconds)", jti, ttlSeconds);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }

        String key = BLACKLIST_PREFIX + jti;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int incrementFailedLogin(String email) {
        String key = FAILED_LOGIN_PREFIX + email.toLowerCase();
        Long count = stringRedisTemplate.opsForValue().increment(key);

        // Set TTL on first increment
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, FAILED_LOGIN_TTL_MINUTES, TimeUnit.MINUTES);
        }

        logger.debug("Failed login attempts for {}: {}", email, count);
        return count != null ? count.intValue() : 0;
    }

    @Override
    public int getFailedLoginCount(String email) {
        String key = FAILED_LOGIN_PREFIX + email.toLowerCase();
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void resetFailedLogin(String email) {
        String key = FAILED_LOGIN_PREFIX + email.toLowerCase();
        stringRedisTemplate.delete(key);
        logger.debug("Reset failed login attempts for {}", email);
    }

    @Override
    public void lockAccount(String email, int minutes) {
        String key = LOCKED_PREFIX + email.toLowerCase();
        stringRedisTemplate.opsForValue().set(key, "locked", minutes, TimeUnit.MINUTES);
        logger.info("Account locked for {} minutes: {}", minutes, email);
    }

    @Override
    public boolean isLocked(String email) {
        String key = LOCKED_PREFIX + email.toLowerCase();
        Boolean locked = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(locked);
    }

    /**
     * Build session Redis key
     */
    private String buildSessionKey(UUID userId, String sessionId) {
        return SESSION_PREFIX + userId.toString() + ":" + sessionId;
    }

    /**
     * Calculate TTL from expiration date
     */
    private long calculateTtl(Date expiration) {
        if (expiration == null) {
            return 86400; // Default 24 hours
        }

        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        return Math.max(ttlMillis / 1000, 60); // Minimum 60 seconds
    }
}
