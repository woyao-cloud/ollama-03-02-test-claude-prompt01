package com.usermanagement.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT Token Provider Test
 *
 * @author Test Team
 * @since 1.0
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private Authentication authentication;
    private UUID userId;
    private UUID deptId;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        // Generate test RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        jwtTokenProvider = new JwtTokenProvider(keyPair);

        // Create test authentication
        userId = UUID.randomUUID();
        deptId = UUID.randomUUID();
        authentication = new UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void shouldGenerateTokenPair() {
        JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(authentication, userId, deptId);

        assertNotNull(tokenPair);
        assertNotNull(tokenPair.accessToken());
        assertNotNull(tokenPair.refreshToken());
        assertEquals(900, tokenPair.expiresIn()); // 15 minutes
        assertFalse(tokenPair.accessToken().isEmpty());
        assertFalse(tokenPair.refreshToken().isEmpty());
    }

    @Test
    void shouldGenerateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken(authentication, userId, deptId);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldGenerateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken(authentication, userId);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldValidateToken() {
        String token = jwtTokenProvider.generateAccessToken(authentication, userId, deptId);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String token = jwtTokenProvider.generateAccessToken(authentication, userId, deptId);
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void shouldExtractRolesFromToken() {
        String token = jwtTokenProvider.generateAccessToken(authentication, userId, deptId);
        Set<String> roles = jwtTokenProvider.getRolesFromToken(token);

        assertNotNull(roles);
        assertTrue(roles.contains("USER"));
    }

    @Test
    void shouldExtractTokenId() {
        String token = jwtTokenProvider.generateAccessToken(authentication, userId, deptId);
        String jti = jwtTokenProvider.getTokenId(token);

        assertNotNull(jti);
        assertFalse(jti.isEmpty());
    }

    @Test
    void shouldExtractTokenType() {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, userId, deptId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, userId);

        assertEquals("access", jwtTokenProvider.getTokenType(accessToken));
        assertEquals("refresh", jwtTokenProvider.getTokenType(refreshToken));
    }

    @Test
    void shouldNotValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.string"));
    }

    @Test
    void shouldNotValidateEmptyToken() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }
}
