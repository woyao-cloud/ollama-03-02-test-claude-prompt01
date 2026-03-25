package com.usermanagement.web.controller;

import com.usermanagement.domain.entity.OAuth2Provider;
import com.usermanagement.security.SecurityUtilsComponent;
import com.usermanagement.service.OAuth2Service;
import com.usermanagement.service.dto.OAuth2ConnectionDTO;
import com.usermanagement.service.dto.OAuth2LoginResult;
import com.usermanagement.service.dto.OAuth2ProviderInfoDTO;
import com.usermanagement.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth2 Controller
 * REST API endpoints for OAuth2 authentication
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/oauth2")
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);

    private final OAuth2Service oauth2Service;
    private final SecurityUtilsComponent securityUtils;

    public OAuth2Controller(OAuth2Service oauth2Service, SecurityUtilsComponent securityUtils) {
        this.oauth2Service = oauth2Service;
        this.securityUtils = securityUtils;
    }

    /**
     * Get supported OAuth2 providers
     */
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<OAuth2ProviderInfoDTO>>> getProviders() {
        logger.debug("Getting supported OAuth2 providers");
        List<OAuth2ProviderInfoDTO> providers = oauth2Service.getSupportedProviders();
        return ResponseEntity.ok(ApiResponse.success("Providers retrieved successfully", providers));
    }

    /**
     * Get authorization URL for a provider
     */
    @GetMapping("/authorize/{provider}")
    public ResponseEntity<ApiResponse<String>> getAuthorizationUrl(
            @PathVariable String provider,
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {

        logger.debug("Getting authorization URL for provider: {}", provider);

        OAuth2Provider oauth2Provider = OAuth2Provider.fromProviderId(provider);
        String authUrl = oauth2Service.getAuthorizationUrl(oauth2Provider, redirectUri, state);

        return ResponseEntity.ok(ApiResponse.success("Authorization URL generated", authUrl));
    }

    /**
     * Redirect to authorization URL
     */
    @GetMapping("/authorize/{provider}/redirect")
    public void redirectToAuthorization(
            @PathVariable String provider,
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        logger.debug("Redirecting to authorization for provider: {}", provider);

        OAuth2Provider oauth2Provider = OAuth2Provider.fromProviderId(provider);
        String authUrl = oauth2Service.getAuthorizationUrl(oauth2Provider, redirectUri, state);

        response.sendRedirect(authUrl);
    }

    /**
     * Handle OAuth2 callback
     */
    @PostMapping("/callback/{provider}")
    public ResponseEntity<ApiResponse<OAuth2LoginResult>> handleCallback(
            @PathVariable String provider,
            @Valid @RequestBody Map<String, String> request) {

        logger.info("Handling OAuth2 callback for provider: {}", provider);

        String code = request.get("code");
        String redirectUri = request.get("redirectUri");

        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Authorization code is required"));
        }

        OAuth2Provider oauth2Provider = OAuth2Provider.fromProviderId(provider);
        OAuth2LoginResult result = oauth2Service.handleCallback(oauth2Provider, code, redirectUri);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success("Authentication successful", result));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(result.getMessage()));
        }
    }

    /**
     * Get current user's OAuth2 connections
     */
    @GetMapping("/connections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OAuth2ConnectionDTO>>> getUserConnections() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        logger.debug("Getting OAuth2 connections for user: {}", currentUserId);

        List<OAuth2ConnectionDTO> connections = oauth2Service.getUserConnections(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Connections retrieved successfully", connections));
    }

    /**
     * Bind OAuth2 account to current user
     */
    @PostMapping("/bind/{provider}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OAuth2ConnectionDTO>> bindAccount(
            @PathVariable String provider,
            @Valid @RequestBody Map<String, String> request) {

        UUID currentUserId = securityUtils.getCurrentUserId();
        logger.info("Binding OAuth2 account for user: {}, provider: {}", currentUserId, provider);

        String code = request.get("code");
        String redirectUri = request.get("redirectUri");

        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Authorization code is required"));
        }

        OAuth2Provider oauth2Provider = OAuth2Provider.fromProviderId(provider);
        OAuth2ConnectionDTO connection = oauth2Service.bindAccount(
                currentUserId, oauth2Provider, code, redirectUri);

        return ResponseEntity.ok(ApiResponse.success("Account bound successfully", connection));
    }

    /**
     * Unbind OAuth2 account
     */
    @DeleteMapping("/connections/{provider}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unbindAccount(@PathVariable String provider) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        logger.info("Unbinding OAuth2 account for user: {}, provider: {}", currentUserId, provider);

        OAuth2Provider oauth2Provider = OAuth2Provider.fromProviderId(provider);
        oauth2Service.unbindAccount(currentUserId, oauth2Provider);

        return ResponseEntity.ok(ApiResponse.success("Account unbound successfully", null));
    }

    /**
     * Set primary OAuth2 connection
     */
    @PutMapping("/connections/{connectionId}/primary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> setPrimaryConnection(
            @PathVariable UUID connectionId) {

        UUID currentUserId = securityUtils.getCurrentUserId();
        logger.info("Setting primary OAuth2 connection: {} for user: {}", connectionId, currentUserId);

        oauth2Service.setPrimaryConnection(currentUserId, connectionId);

        return ResponseEntity.ok(ApiResponse.success("Primary connection set successfully", null));
    }

    /**
     * Check if OAuth2 account exists
     */
    @GetMapping("/check/{provider}")
    public ResponseEntity<ApiResponse<Boolean>> checkAccountExists(
            @PathVariable String provider,
            @RequestParam String providerUserId) {

        OAuth2Provider oauth2Provider = OAuth2Provider.fromProviderId(provider);
        boolean exists = oauth2Service.accountExists(oauth2Provider, providerUserId);

        return ResponseEntity.ok(ApiResponse.success("Account existence checked", exists));
    }
}
