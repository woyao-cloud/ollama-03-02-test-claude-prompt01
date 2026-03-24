package com.usermanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Security Utils Component
 * Spring component version of SecurityUtils for use in SpEL expressions and dependency injection
 *
 * @author Security Team
 * @since 1.0
 */
@Component("securityUtils")
public class SecurityUtilsComponent {

    /**
     * Get current user ID from authentication
     */
    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                return UUID.fromString(username);
            } catch (IllegalArgumentException e) {
                // Username is not a UUID (might be email), return a default or throw
                throw new IllegalStateException("User ID not found in authentication");
            }
        }

        throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
    }

    /**
     * Check if the given user ID is the current authenticated user
     * Used in SpEL: @securityUtils.isCurrentUser(#id)
     */
    public boolean isCurrentUser(UUID userId) {
        try {
            UUID currentUserId = getCurrentUserId();
            return currentUserId.equals(userId);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Check if the given user ID string is the current authenticated user
     */
    public boolean isCurrentUser(String userId) {
        try {
            return isCurrentUser(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if current user has specific authority
     */
    public boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * Check if current user has any of the specified authorities
     */
    public boolean hasAnyAuthority(String... authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        for (String authority : authorities) {
            if (auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() != null;
    }

    /**
     * Get current username (email)
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        return principal.toString();
    }
}
