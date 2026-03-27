package com.usermanagement.service.oauth2;

import com.usermanagement.domain.entity.OAuth2Provider;
import com.usermanagement.service.dto.OAuth2UserProfile;

/**
 * OAuth2 Client Interface
 * Abstract interface for OAuth2 provider implementations
 *
 * @author Service Team
 * @since 1.0
 */
public interface OAuth2Client {

    /**
     * Get the OAuth2 provider type
     *
     * @return OAuth2 provider
     */
    OAuth2Provider getProvider();

    /**
     * Get authorization URL
     *
     * @param redirectUri redirect URI
     * @param state state parameter
     * @return authorization URL
     */
    String getAuthorizationUrl(String redirectUri, String state);

    /**
     * Exchange authorization code for access token
     *
     * @param code authorization code
     * @param redirectUri redirect URI
     * @return access token
     */
    String exchangeCodeForToken(String code, String redirectUri);

    /**
     * Get user profile using access token
     *
     * @param accessToken access token
     * @return user profile
     */
    OAuth2UserProfile getUserProfile(String accessToken);

    /**
     * Refresh access token
     *
     * @param refreshToken refresh token
     * @return new access token
     */
    String refreshAccessToken(String refreshToken);

    /**
     * Check if client is configured
     *
     * @return true if client ID and secret are configured
     */
    boolean isConfigured();
}
