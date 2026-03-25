package com.usermanagement.service;

import com.usermanagement.domain.entity.OAuth2Provider;
import com.usermanagement.service.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * OAuth2 Service Interface
 * Handles OAuth2 authentication and account binding
 *
 * @author Service Team
 * @since 1.0
 */
public interface OAuth2Service {

    /**
     * Get OAuth2 authorization URL for a provider
     *
     * @param provider OAuth2 provider
     * @param redirectUri redirect URI after authorization
     * @param state state parameter for CSRF protection
     * @return authorization URL
     */
    String getAuthorizationUrl(OAuth2Provider provider, String redirectUri, String state);

    /**
     * Handle OAuth2 callback and authenticate user
     *
     * @param provider OAuth2 provider
     * @param code authorization code
     * @param redirectUri redirect URI
     * @return authentication result with tokens
     */
    OAuth2LoginResult handleCallback(OAuth2Provider provider, String code, String redirectUri);

    /**
     * Bind OAuth2 account to existing user
     *
     * @param userId user ID
     * @param provider OAuth2 provider
     * @param code authorization code
     * @param redirectUri redirect URI
     * @return bound OAuth2 connection
     */
    OAuth2ConnectionDTO bindAccount(UUID userId, OAuth2Provider provider, String code, String redirectUri);

    /**
     * Unbind OAuth2 account from user
     *
     * @param userId user ID
     * @param provider OAuth2 provider
     */
    void unbindAccount(UUID userId, OAuth2Provider provider);

    /**
     * Get user's OAuth2 connections
     *
     * @param userId user ID
     * @return list of OAuth2 connections
     */
    List<OAuth2ConnectionDTO> getUserConnections(UUID userId);

    /**
     * Get OAuth2 connection by ID
     *
     * @param connectionId connection ID
     * @return OAuth2 connection
     */
    OAuth2ConnectionDTO getConnection(UUID connectionId);

    /**
     * Set primary OAuth2 connection
     *
     * @param userId user ID
     * @param connectionId connection ID
     */
    void setPrimaryConnection(UUID userId, UUID connectionId);

    /**
     * Check if OAuth2 account exists
     *
     * @param provider OAuth2 provider
     * @param providerUserId provider user ID
     * @return true if exists
     */
    boolean accountExists(OAuth2Provider provider, String providerUserId);

    /**
     * Find user by OAuth2 account
     *
     * @param provider OAuth2 provider
     * @param providerUserId provider user ID
     * @return user DTO if found
     */
    UserDTO findUserByOAuth2Account(OAuth2Provider provider, String providerUserId);

    /**
     * Get supported OAuth2 providers
     *
     * @return list of supported providers
     */
    List<OAuth2ProviderInfoDTO> getSupportedProviders();

    /**
     * Refresh OAuth2 access token
     *
     * @param connectionId connection ID
     * @return true if refreshed successfully
     */
    boolean refreshToken(UUID connectionId);

    /**
     * Create user from OAuth2 profile
     *
     * @param profile OAuth2 user profile
     * @return created user DTO
     */
    UserDTO createUserFromOAuth2Profile(OAuth2UserProfile profile);
}
