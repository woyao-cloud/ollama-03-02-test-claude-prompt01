package com.usermanagement.service.dto;

import com.usermanagement.domain.entity.OAuth2Provider;

/**
 * OAuth2 Provider Info DTO
 * Information about supported OAuth2 provider
 *
 * @author Service Team
 * @since 1.0
 */
public class OAuth2ProviderInfoDTO {

    private OAuth2Provider provider;
    private String displayName;
    private String providerId;
    private boolean enabled;
    private String authorizationUrl;
    private String iconUrl;

    // Constructors
    public OAuth2ProviderInfoDTO() {}

    public OAuth2ProviderInfoDTO(OAuth2Provider provider, boolean enabled) {
        this.provider = provider;
        this.displayName = provider.getDisplayName();
        this.providerId = provider.getProviderId();
        this.enabled = enabled;
    }

    // Getters and Setters

    public OAuth2Provider getProvider() {
        return provider;
    }

    public void setProvider(OAuth2Provider provider) {
        this.provider = provider;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
