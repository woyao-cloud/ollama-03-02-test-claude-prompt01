package com.usermanagement.domain.entity;

/**
 * OAuth2 Provider Enum
 * Supported OAuth2 providers
 *
 * @author Security Team
 * @since 1.0
 */
public enum OAuth2Provider {
    GOOGLE("Google", "google"),
    GITHUB("GitHub", "github"),
    MICROSOFT("Microsoft", "microsoft"),
    WECHAT("WeChat", "wechat"),
    DINGTALK("DingTalk", "dingtalk"),
    FEISHU("Feishu", "feishu");

    private final String displayName;
    private final String providerId;

    OAuth2Provider(String displayName, String providerId) {
        this.displayName = displayName;
        this.providerId = providerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProviderId() {
        return providerId;
    }

    /**
     * Get provider by provider ID
     */
    public static OAuth2Provider fromProviderId(String providerId) {
        for (OAuth2Provider provider : values()) {
            if (provider.getProviderId().equalsIgnoreCase(providerId)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth2 provider: " + providerId);
    }

    /**
     * Check if provider is supported
     */
    public static boolean isSupported(String providerId) {
        for (OAuth2Provider provider : values()) {
            if (provider.getProviderId().equalsIgnoreCase(providerId)) {
                return true;
            }
        }
        return false;
    }
}
