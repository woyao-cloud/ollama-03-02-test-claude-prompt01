package com.usermanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Session Service Test
 *
 * @author Test Team
 * @since 1.0
 */
class SessionServiceImplTest {

    private SessionService sessionService;
    private RedisTemplate<String, Object> redisTemplate;
    private RedisTemplate<String, String> stringRedisTemplate;
    private ValueOperations<String, Object> valueOps;
    private ValueOperations<String, String> stringValueOps;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        stringRedisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        stringValueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOps);

        sessionService = new SessionServiceImpl(redisTemplate, stringRedisTemplate);
    }

    @Test
    void shouldCreateSession() {
        UUID userId = UUID.randomUUID();
        String sessionId = "test-session";
        String accessJti = "access-jti";
        String refreshJti = "refresh-jti";

        sessionService.createSession(userId, sessionId, accessJti, refreshJti, 3600);

        verify(valueOps).set(anyString(), any(SessionService.SessionData.class), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldDeleteSession() {
        UUID userId = UUID.randomUUID();
        String sessionId = "test-session";

        sessionService.deleteSession(userId, sessionId);

        verify(redisTemplate).delete(contains(sessionId));
    }

    @Test
    void shouldAddToBlacklist() {
        String jti = "test-jti";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        sessionService.addToBlacklist(jti, expiration);

        verify(stringValueOps).set(contains(jti), eq("blacklisted"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldCheckBlacklist() {
        String jti = "test-jti";
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = sessionService.isBlacklisted(jti);

        assertTrue(result);
        verify(stringRedisTemplate).hasKey(contains(jti));
    }

    @Test
    void shouldNotBeBlacklistedWhenNotInRedis() {
        String jti = "test-jti";
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        boolean result = sessionService.isBlacklisted(jti);

        assertFalse(result);
    }

    @Test
    void shouldIncrementFailedLogin() {
        String email = "test@example.com";
        when(stringValueOps.increment(anyString())).thenReturn(1L);

        int count = sessionService.incrementFailedLogin(email);

        assertEquals(1, count);
        verify(stringValueOps).increment(contains(email.toLowerCase()));
    }

    @Test
    void shouldResetFailedLogin() {
        String email = "test@example.com";

        sessionService.resetFailedLogin(email);

        verify(stringRedisTemplate).delete(contains(email.toLowerCase()));
    }

    @Test
    void shouldLockAccount() {
        String email = "test@example.com";

        sessionService.lockAccount(email, 30);

        verify(stringValueOps).set(contains(email.toLowerCase()), eq("locked"), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldCheckLockedStatus() {
        String email = "test@example.com";
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        boolean locked = sessionService.isLocked(email);

        assertTrue(locked);
    }

    @Test
    void shouldReturnNotLockedWhenNotInRedis() {
        String email = "test@example.com";
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        boolean locked = sessionService.isLocked(email);

        assertFalse(locked);
    }

    @Test
    void shouldHandleNullJtiForBlacklist() {
        sessionService.addToBlacklist(null, new Date());
        verify(stringValueOps, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void shouldReturnFalseForNullJtiIsBlacklisted() {
        boolean result = sessionService.isBlacklisted(null);
        assertFalse(result);
    }
}
