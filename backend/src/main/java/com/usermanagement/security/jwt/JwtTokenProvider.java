package com.usermanagement.security.jwt;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT Token Provider
 * Generates and manages JWT tokens with RSA256 signing
 *
 * @author Security Team
 * @since 1.0
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String ISSUER = "usermanagement";
    private static final String AUDIENCE = "usermanagement-api";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_DEPT_ID = "deptId";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final long accessTokenExpirationMinutes = 15;
    private final long refreshTokenExpirationDays = 7;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtTokenProvider(KeyPair jwtKeyPair) {
        this.privateKey = jwtKeyPair.getPrivate();
        this.publicKey = jwtKeyPair.getPublic();
        logger.info("JWT Token Provider initialized with RSA key pair");
    }

    /**
     * Generate access token
     */
    public TokenPair generateTokenPair(org.springframework.security.core.Authentication authentication, UUID userId, UUID deptId) {
        String accessToken = generateAccessToken(authentication, userId, deptId);
        String refreshToken = generateRefreshToken(authentication, userId);

        return new TokenPair(accessToken, refreshToken, accessTokenExpirationMinutes * 60);
    }

    /**
     * Generate access token
     */
    public String generateAccessToken(org.springframework.security.core.Authentication authentication, UUID userId, UUID deptId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();

        Set<String> roles = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .collect(Collectors.toSet());

        Set<String> permissions = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(new Date(now.toEpochMilli()))
                .expiration(new Date(expiry.toEpochMilli()))
                .id(jti)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, permissions)
                .claim(CLAIM_DEPT_ID, deptId != null ? deptId.toString() : null)
                .claim("type", TOKEN_TYPE_ACCESS)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(org.springframework.security.core.Authentication authentication, UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(new Date(now.toEpochMilli()))
                .expiration(new Date(expiry.toEpochMilli()))
                .id(jti)
                .claim("type", TOKEN_TYPE_REFRESH)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Parse token claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Get token ID (jti)
     */
    public String getTokenId(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDate(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * Get roles from token
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return roles != null ? new HashSet<>(roles) : Collections.emptySet();
    }

    /**
     * Get permissions from token
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        List<String> permissions = claims.get(CLAIM_PERMISSIONS, List.class);
        return permissions != null ? new HashSet<>(permissions) : Collections.emptySet();
    }

    /**
     * Get token type (access or refresh)
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Token pair record
     */
    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}
}
