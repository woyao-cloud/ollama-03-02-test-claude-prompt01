package com.usermanagement.service;

import com.usermanagement.domain.entity.OAuth2Connection;
import com.usermanagement.domain.entity.OAuth2Provider;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.OAuth2ConnectionRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.dto.*;
import com.usermanagement.service.oauth2.OAuth2Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth2 Service Implementation
 * Handles OAuth2 authentication and account binding
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional
public class OAuth2ServiceImpl implements OAuth2Service {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ServiceImpl.class);

    private final OAuth2ConnectionRepository oAuth2ConnectionRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final List<OAuth2Client> oauth2Clients;

    public OAuth2ServiceImpl(OAuth2ConnectionRepository oAuth2ConnectionRepository,
                             UserRepository userRepository,
                             TokenService tokenService,
                             List<OAuth2Client> oauth2Clients) {
        this.oAuth2ConnectionRepository = oAuth2ConnectionRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.oauth2Clients = oauth2Clients;
    }

    @Override
    public String getAuthorizationUrl(OAuth2Provider provider, String redirectUri, String state) {
        OAuth2Client client = getClient(provider);
        if (client == null || !client.isConfigured()) {
            throw new IllegalArgumentException("OAuth2 provider not configured: " + provider);
        }
        return client.getAuthorizationUrl(redirectUri, state);
    }

    @Override
    public OAuth2LoginResult handleCallback(OAuth2Provider provider, String code, String redirectUri) {
        try {
            OAuth2Client client = getClient(provider);
            if (client == null) {
                return OAuth2LoginResult.failure("Provider not supported: " + provider);
            }

            // Exchange code for access token
            String accessToken = client.exchangeCodeForToken(code, redirectUri);
            if (accessToken == null) {
                return OAuth2LoginResult.failure("Failed to exchange authorization code");
            }

            // Get user profile
            OAuth2UserProfile profile = client.getUserProfile(accessToken);
            if (profile == null) {
                return OAuth2LoginResult.failure("Failed to fetch user profile");
            }

            profile.setProvider(provider);

            // Find existing connection
            Optional<OAuth2Connection> existingConnection =
                    oAuth2ConnectionRepository.findByProviderAndProviderUserId(provider, profile.getProviderUserId());

            if (existingConnection.isPresent()) {
                // Existing user - update connection and login
                return loginExistingUser(existingConnection.get(), profile, accessToken);
            } else {
                // Check if user exists with same email
                Optional<User> existingUser = userRepository.findByEmail(profile.getEmail());

                if (existingUser.isPresent()) {
                    // Bind to existing account
                    return bindToExistingUser(existingUser.get(), profile, accessToken);
                } else {
                    // Create new user
                    return createNewUser(profile, accessToken);
                }
            }

        } catch (Exception e) {
            logger.error("OAuth2 callback error", e);
            return OAuth2LoginResult.failure("Authentication failed: " + e.getMessage());
        }
    }

    @Override
    public OAuth2ConnectionDTO bindAccount(UUID userId, OAuth2Provider provider, String code, String redirectUri) {
        OAuth2Client client = getClient(provider);
        if (client == null) {
            throw new IllegalArgumentException("Provider not supported: " + provider);
        }

        String accessToken = client.exchangeCodeForToken(code, redirectUri);
        if (accessToken == null) {
            throw new IllegalStateException("Failed to exchange authorization code");
        }

        OAuth2UserProfile profile = client.getUserProfile(accessToken);
        if (profile == null) {
            throw new IllegalStateException("Failed to fetch user profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if already bound
        if (oAuth2ConnectionRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new IllegalStateException("Account already bound to this provider");
        }

        OAuth2Connection connection = createConnection(user, profile, accessToken, false);
        return convertToDTO(connection);
    }

    @Override
    public void unbindAccount(UUID userId, OAuth2Provider provider) {
        oAuth2ConnectionRepository.deleteByUserIdAndProvider(userId, provider);
        logger.info("Unbound {} OAuth2 account for user {}", provider, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuth2ConnectionDTO> getUserConnections(UUID userId) {
        return oAuth2ConnectionRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2ConnectionDTO getConnection(UUID connectionId) {
        OAuth2Connection connection = oAuth2ConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        return convertToDTO(connection);
    }

    @Override
    public void setPrimaryConnection(UUID userId, UUID connectionId) {
        // Reset all primary connections for user
        List<OAuth2Connection> connections = oAuth2ConnectionRepository.findByUserId(userId);
        for (OAuth2Connection conn : connections) {
            conn.setIsPrimary(false);
        }

        // Set new primary
        OAuth2Connection connection = oAuth2ConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        if (!connection.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Connection does not belong to user");
        }

        connection.setIsPrimary(true);
        oAuth2ConnectionRepository.save(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean accountExists(OAuth2Provider provider, String providerUserId) {
        return oAuth2ConnectionRepository.existsByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findUserByOAuth2Account(OAuth2Provider provider, String providerUserId) {
        return oAuth2ConnectionRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(conn -> convertToUserDTO(conn.getUser()))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuth2ProviderInfoDTO> getSupportedProviders() {
        return Arrays.stream(OAuth2Provider.values())
                .map(provider -> {
                    OAuth2Client client = getClient(provider);
                    boolean enabled = client != null && client.isConfigured();
                    return new OAuth2ProviderInfoDTO(provider, enabled);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean refreshToken(UUID connectionId) {
        OAuth2Connection connection = oAuth2ConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

        if (connection.getRefreshToken() == null) {
            return false;
        }

        OAuth2Client client = getClient(connection.getProvider());
        if (client == null) {
            return false;
        }

        String newToken = client.refreshAccessToken(connection.getRefreshToken());
        if (newToken != null) {
            connection.setAccessToken(newToken);
            connection.setTokenExpiresAt(Instant.now().plusSeconds(3600));
            oAuth2ConnectionRepository.save(connection);
            return true;
        }

        return false;
    }

    @Override
    public UserDTO createUserFromOAuth2Profile(OAuth2UserProfile profile) {
        User user = new User();
        user.setEmail(profile.getEmail());
        user.setFirstName(profile.getFirstName() != null ? profile.getFirstName() : "New");
        user.setLastName(profile.getLastName() != null ? profile.getLastName() : "User");
        user.setAvatarUrl(profile.getAvatarUrl());
        user.setStatus(User.UserStatus.PENDING);
        user.setEmailVerified(true); // OAuth2 emails are verified

        User savedUser = userRepository.save(user);
        return convertToUserDTO(savedUser);
    }

    // Helper methods

    private OAuth2Client getClient(OAuth2Provider provider) {
        return oauth2Clients.stream()
                .filter(client -> client.getProvider() == provider)
                .findFirst()
                .orElse(null);
    }

    private OAuth2LoginResult loginExistingUser(OAuth2Connection connection, OAuth2UserProfile profile, String accessToken) {
        User user = connection.getUser();

        // Update connection
        connection.setAccessToken(accessToken);
        connection.setProviderUsername(profile.getUsername());
        connection.setDisplayName(profile.getDisplayName());
        connection.setEmail(profile.getEmail());
        connection.setAvatarUrl(profile.getAvatarUrl());
        connection.recordLogin();
        oAuth2ConnectionRepository.save(connection);

        // Update user info
        if (user.getAvatarUrl() == null && profile.getAvatarUrl() != null) {
            user.setAvatarUrl(profile.getAvatarUrl());
            userRepository.save(user);
        }

        // Generate tokens
        String accessTokenStr = tokenService.generateAccessToken(user.getId());
        String refreshTokenStr = tokenService.generateRefreshToken(user.getId());

        return OAuth2LoginResult.success(
                user.getId(),
                user.getEmail(),
                accessTokenStr,
                refreshTokenStr,
                3600L,
                false,
                connection.getProvider()
        );
    }

    private OAuth2LoginResult bindToExistingUser(User user, OAuth2UserProfile profile, String accessToken) {
        OAuth2Connection connection = createConnection(user, profile, accessToken, false);

        String accessTokenStr = tokenService.generateAccessToken(user.getId());
        String refreshTokenStr = tokenService.generateRefreshToken(user.getId());

        return OAuth2LoginResult.success(
                user.getId(),
                user.getEmail(),
                accessTokenStr,
                refreshTokenStr,
                3600L,
                false,
                profile.getProvider()
        );
    }

    private OAuth2LoginResult createNewUser(OAuth2UserProfile profile, String accessToken) {
        UserDTO userDTO = createUserFromOAuth2Profile(profile);
        User user = userRepository.findById(userDTO.getId()).orElseThrow();

        OAuth2Connection connection = createConnection(user, profile, accessToken, true);

        String accessTokenStr = tokenService.generateAccessToken(user.getId());
        String refreshTokenStr = tokenService.generateRefreshToken(user.getId());

        return OAuth2LoginResult.success(
                user.getId(),
                user.getEmail(),
                accessTokenStr,
                refreshTokenStr,
                3600L,
                true,
                profile.getProvider()
        );
    }

    private OAuth2Connection createConnection(User user, OAuth2UserProfile profile, String accessToken, boolean isPrimary) {
        OAuth2Connection connection = new OAuth2Connection();
        connection.setUser(user);
        connection.setProvider(profile.getProvider());
        connection.setProviderUserId(profile.getProviderUserId());
        connection.setProviderUsername(profile.getUsername());
        connection.setDisplayName(profile.getDisplayName());
        connection.setEmail(profile.getEmail());
        connection.setAvatarUrl(profile.getAvatarUrl());
        connection.setAccessToken(accessToken);
        connection.setIsPrimary(isPrimary);
        connection.recordLogin();

        if (profile.getExpiresIn() != null) {
            connection.setTokenExpiresAt(Instant.now().plusSeconds(profile.getExpiresIn()));
        }

        return oAuth2ConnectionRepository.save(connection);
    }

    private OAuth2ConnectionDTO convertToDTO(OAuth2Connection connection) {
        OAuth2ConnectionDTO dto = new OAuth2ConnectionDTO();
        dto.setId(connection.getId());
        dto.setProvider(connection.getProvider());
        dto.setProviderDisplayName(connection.getProvider().getDisplayName());
        dto.setProviderUsername(connection.getProviderUsername());
        dto.setDisplayName(connection.getDisplayName());
        dto.setEmail(connection.getEmail());
        dto.setAvatarUrl(connection.getAvatarUrl());
        dto.setIsPrimary(connection.getIsPrimary());
        dto.setLastLoginAt(connection.getLastLoginAt());
        dto.setCreatedAt(connection.getCreatedAt());
        dto.setUpdatedAt(connection.getUpdatedAt());
        return dto;
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setStatus(user.getStatus());
        dto.setAvatarUrl(user.getAvatarUrl());
        return dto;
    }
}
