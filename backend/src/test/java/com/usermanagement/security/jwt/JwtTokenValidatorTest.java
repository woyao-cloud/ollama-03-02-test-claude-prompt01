package com.usermanagement.security.jwt;

import com.usermanagement.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JWT Token Validator Test
 *
 * @author Test Team
 * @since 1.0
 */
class JwtTokenValidatorTest {

    private JwtTokenValidator jwtTokenValidator;
    private JwtTokenProvider jwtTokenProvider;
    private SessionService sessionService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        sessionService = mock(SessionService.class);
        jwtTokenValidator = new JwtTokenValidator(keyPair, sessionService);
        jwtTokenProvider = new JwtTokenProvider(keyPair);
    }

    @Test
    void shouldValidateValidToken() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = jwtTokenProvider.generateAccessToken(auth, userId, null);
        when(sessionService.isBlacklisted(any())).thenReturn(false);

        Optional<Authentication> result = jwtTokenValidator.validateTokenAndGetAuth(token);

        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotValidateBlacklistedToken() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.emptyList()
        );

        String token = jwtTokenProvider.generateAccessToken(auth, userId, null);
        when(sessionService.isBlacklisted(any())).thenReturn(true);

        Optional<Authentication> result = jwtTokenValidator.validateTokenAndGetAuth(token);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldExtractUserId() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.emptyList()
        );

        String token = jwtTokenProvider.generateAccessToken(auth, userId, null);
        UUID extracted = jwtTokenValidator.getUserIdFromToken(token);

        assertEquals(userId, extracted);
    }

    @Test
    void shouldExtractTokenId() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.emptyList()
        );

        String token = jwtTokenProvider.generateAccessToken(auth, userId, null);
        String jti = jwtTokenValidator.getTokenId(token);

        assertNotNull(jti);
        assertFalse(jti.isEmpty());
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        assertNull(jwtTokenValidator.getUserIdFromToken("invalid"));
    }

    @Test
    void shouldCheckTokenExpiration() {
        assertTrue(jwtTokenValidator.isTokenExpired("invalid"));
    }
}
