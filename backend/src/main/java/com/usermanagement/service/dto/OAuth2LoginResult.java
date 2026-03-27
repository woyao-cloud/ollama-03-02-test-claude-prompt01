package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.OAuth2Provider;

import java.util.UUID;

/**
 * OAuth2 Login Result
 * Result of OAuth2 authentication process
 *
 * @author Service Team
 * @since 1.0
 */
public class OAuth2LoginResult {

    private boolean success;
    private UUID userId;
    private String email;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private boolean isNewUser;
    private OAuth2Provider provider;
    private String message;

    // Constructors
    public OAuth2LoginResult() {}

    public static OAuth2LoginResult success(UUID userId, String email, String accessToken,
                                            String refreshToken, Long expiresIn, boolean isNewUser,
                                            OAuth2Provider provider) {
        OAuth2LoginResult result = new OAuth2LoginResult();
        result.success = true;
        result.userId = userId;
        result.email = email;
        result.accessToken = accessToken;
        result.refreshToken = refreshToken;
        result.expiresIn = expiresIn;
        result.isNewUser = isNewUser;
        result.provider = provider;
        result.message = "Login successful";
        return result;
    }

    public static OAuth2LoginResult failure(String message) {
        OAuth2LoginResult result = new OAuth2LoginResult();
        result.success = false;
        result.message = message;
        return result;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean newUser) {
        isNewUser = newUser;
    }

    public OAuth2Provider getProvider() {
        return provider;
    }

    public void setProvider(OAuth2Provider provider) {
        this.provider = provider;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
