package com.usermanagement.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * OAuth2 Connection Entity
 * Stores OAuth2 provider connections for users
 *
 * @author Security Team
 * @since 1.0
 */
@Entity
@Table(name = "oauth2_connections", indexes = {
    @Index(name = "idx_oauth2_user", columnList = "user_id"),
    @Index(name = "idx_oauth2_provider", columnList = "provider"),
    @Index(name = "idx_oauth2_provider_user", columnList = "provider, provider_user_id", unique = true)
})
public class OAuth2Connection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_oauth2_user"))
    private User user;

    @Column(name = "provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OAuth2Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "provider_username", length = 100)
    private String providerUsername;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // Getters and Setters

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OAuth2Provider getProvider() {
        return provider;
    }

    public void setProvider(OAuth2Provider provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getProviderUsername() {
        return providerUsername;
    }

    public void setProviderUsername(String providerUsername) {
        this.providerUsername = providerUsername;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // Business methods

    /**
     * Check if token is expired
     */
    @Transient
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            return false;
        }
        return Instant.now().isAfter(tokenExpiresAt);
    }

    /**
     * Check if this is the primary connection
     */
    @Transient
    public boolean isPrimary() {
        return isPrimary != null && isPrimary;
    }

    /**
     * Record login
     */
    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    @Override
    public String toString() {
        return "OAuth2Connection{" +
                "id=" + getId() +
                ", provider=" + provider +
                ", providerUserId='" + providerUserId + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
