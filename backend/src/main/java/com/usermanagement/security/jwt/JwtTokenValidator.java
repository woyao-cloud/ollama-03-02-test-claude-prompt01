package com.usermanagement.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT Token Validator
 * Validates JWT tokens and extracts authentication information
 *
 * @author Security Team
 * @since 1.0
 */
@Component
public class JwtTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final PublicKey publicKey;
    private final com.usermanagement.service.SessionService sessionService;

    public JwtTokenValidator(KeyPair jwtKeyPair,
                             com.usermanagement.service.SessionService sessionService) {
        this.publicKey = jwtKeyPair.getPublic();
        this.sessionService = sessionService;
    }

    /**
     * Validate token and return authentication
     */
    public Optional<Authentication> validateTokenAndGetAuth(String token) {
        try {
            // First check if token is blacklisted
            String jti = getTokenId(token);
            if (sessionService.isBlacklisted(jti)) {
                logger.warn("Token is blacklisted: {}", jti);
                return Optional.empty();
            }

            // Parse and validate token
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check expiration
            if (claims.getExpiration().before(new Date())) {
                logger.warn("Token is expired");
                return Optional.empty();
            }

            // Build authentication from claims
            Authentication authentication = buildAuthentication(claims);
            return Optional.of(authentication);

        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Build authentication from JWT claims
     */
    private Authentication buildAuthentication(Claims claims) {
        String userId = claims.getSubject();

        // Extract roles
        List<String> roles = claims.get("roles", List.class);
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        // Extract permissions
        List<String> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
            permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
        }

        UserDetails userDetails = User.builder()
                .username(userId)
                .password("") // Not needed for JWT
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                authorities
        );
    }

    /**
     * Get token ID (jti)
     */
    public String getTokenId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (Exception e) {
            logger.error("Failed to get token ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            logger.error("Failed to get user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error("Failed to get expiration date: {}", e.getMessage());
            return null;
        }
    }
}
