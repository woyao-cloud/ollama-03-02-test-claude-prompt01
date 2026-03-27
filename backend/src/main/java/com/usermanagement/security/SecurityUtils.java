package com.usermanagement.security;

import com.usermanagement.domain.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * Security Utility Class
 * Provides helper methods for security operations
 *
 * @author Security Team
 * @since 1.0
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Get current authentication
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Get current user ID from authentication
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserDetails)
                .map(principal -> {
                    UserDetails userDetails = (UserDetails) principal;
                    try {
                        return UUID.fromString(userDetails.getUsername());
                    } catch (IllegalArgumentException e) {
                        // If username is not a UUID, it might be an email
                        // In that case, return null and let caller handle it
                        return null;
                    }
                });
    }

    /**
     * Get current username (email) from authentication
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserDetails)
                .map(principal -> ((UserDetails) principal).getUsername());
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentAuthentication()
                .map(Authentication::isAuthenticated)
                .orElse(false);
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(grantedAuthority ->
                                grantedAuthority.getAuthority().equals("ROLE_" + role)))
                .orElse(false);
    }

    /**
     * Check if current user has a specific permission
     */
    public static boolean hasPermission(String permission) {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(grantedAuthority ->
                                grantedAuthority.getAuthority().equals(permission)))
                .orElse(false);
    }

    /**
     * Check if user is anonymous (not authenticated)
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }

    /**
     * Get current user's IP address (placeholder - to be used with request context)
     */
    public static String getCurrentUserIp() {
        // This would require request context
        // For now, return a placeholder
        return "0.0.0.0";
    }
}
