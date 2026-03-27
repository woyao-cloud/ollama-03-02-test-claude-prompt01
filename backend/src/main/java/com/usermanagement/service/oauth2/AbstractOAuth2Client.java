package com.usermanagement.service.oauth2;

import com.usermanagement.domain.entity.OAuth2Provider;
import com.usermanagement.service.dto.OAuth2UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Abstract OAuth2 Client
 * Base implementation for OAuth2 clients
 *
 * @author Service Team
 * @since 1.0
 */
public abstract class AbstractOAuth2Client implements OAuth2Client {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final RestTemplate restTemplate;

    protected String clientId;
    protected String clientSecret;
    protected String authorizationUri;
    protected String tokenUri;
    protected String userInfoUri;
    protected String scope;

    public AbstractOAuth2Client() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getAuthorizationUrl(String redirectUri, String state) {
        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                authorizationUri,
                clientId,
                redirectUri,
                scope,
                state);
    }

    @Override
    public String exchangeCodeForToken(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            logger.error("Failed to exchange code for token: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            logger.error("Error exchanging code for token", e);
            return null;
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);
            params.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            return null;

        } catch (Exception e) {
            logger.error("Error refreshing access token", e);
            return null;
        }
    }

    @Override
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }

    /**
     * Extract user profile from provider-specific response
     */
    protected abstract OAuth2UserProfile extractUserProfile(Map<String, Object> attributes);

    /**
     * Make authenticated request to user info endpoint
     */
    protected Map<String, Object> fetchUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    request,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            return null;

        } catch (Exception e) {
            logger.error("Error fetching user info", e);
            return null;
        }
    }

    // Getters and Setters for configuration

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
