package com.usermanagement.service;

import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.jwt.JwtTokenProvider;
import com.usermanagement.web.dto.LoginRequest;
import com.usermanagement.web.dto.LoginResponse;
import com.usermanagement.web.dto.RefreshTokenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication Service Implementation
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final PasswordPolicyService passwordPolicyService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          SessionService sessionService,
                          PasswordPolicyService passwordPolicyService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        logger.info("Login attempt for user: {}", loginRequest.getEmail());

        String email = loginRequest.getEmail().toLowerCase().trim();

        // Check if account is locked
        if (sessionService.isLocked(email)) {
            logger.warn("Account is locked for user: {}", email);
            throw new RuntimeException("Account is locked. Please try again later.");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            User user = userRepository.findActiveByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user is active
            if (!user.isActive()) {
                throw new RuntimeException("Account is not active");
            }

            // Generate tokens
            UUID userId = user.getId();
            UUID deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;

            JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(authentication, userId, deptId);

            // Create session in Redis
            String sessionId = UUID.randomUUID().toString();
            String accessJti = jwtTokenProvider.getTokenId(tokenPair.accessToken());
            String refreshJti = jwtTokenProvider.getTokenId(tokenPair.refreshToken());

            long refreshExpirationSeconds = loginRequest.isRememberMe() ? 30L * 24 * 60 * 60 : 7L * 24 * 60 * 60;

            sessionService.createSession(
                    userId,
                    sessionId,
                    accessJti,
                    refreshJti,
                    refreshExpirationSeconds
            );

            // Reset failed login attempts
            sessionService.resetFailedLogin(email);

            // Update user login info
            user.recordLoginSuccess(ipAddress);
            userRepository.save(user);

            // Build response
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5))
                    .collect(Collectors.toSet());

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    userId,
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    roles
            );

            logger.info("Login successful for user: {}", email);

            return new LoginResponse(
                    tokenPair.accessToken(),
                    tokenPair.refreshToken(),
                    "Bearer",
                    tokenPair.expiresIn(),
                    userInfo
            );

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            int failedAttempts = sessionService.incrementFailedLogin(email);
            logger.warn("Login failed for user: {}. Failed attempts: {}", email, failedAttempts);

            if (failedAttempts >= 5) {
                sessionService.lockAccount(email, 30);
                throw new RuntimeException("Account locked due to too many failed attempts. Please try again after 30 minutes.");
            }

            throw new RuntimeException("Invalid email or password");
        } catch (LockedException e) {
            logger.warn("Account locked for user: {}", email);
            throw new RuntimeException("Account is locked. Please try again later.");
        } catch (DisabledException e) {
            logger.warn("Account disabled for user: {}", email);
            throw new RuntimeException("Account is disabled");
        }
    }

    @Override
    @Transactional
    public void logout(String token) {
        logger.info("Logout request");

        if (token != null && !token.isEmpty()) {
            try {
                // Get token JTI and add to blacklist
                String jti = jwtTokenProvider.getTokenId(token);
                if (jti != null) {
                    sessionService.addToBlacklist(jti, jwtTokenProvider.getExpirationDate(token));
                    logger.info("Token added to blacklist: {}", jti);
                }

                // Get user ID and delete session
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    sessionService.deleteSession(userId, jti);
                }

            } catch (Exception e) {
                logger.error("Error during logout: {}", e.getMessage());
            }
        }

        SecurityContextHolder.clearContext();
        logger.info("Logout successful");
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Token refresh request");

        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Check if refresh token is blacklisted
        String jti = jwtTokenProvider.getTokenId(refreshToken);
        if (sessionService.isBlacklisted(jti)) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Get user from token
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is not active");
        }

        // Build authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getUserRoles().stream()
                        .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                        .map(rp -> (GrantedAuthority) () -> rp.getPermission().getCode())
                        .collect(Collectors.toList())
        );

        // Generate new tokens
        UUID deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(authentication, userId, deptId);

        // Update session
        String newAccessJti = jwtTokenProvider.getTokenId(tokenPair.accessToken());
        String newRefreshJti = jwtTokenProvider.getTokenId(tokenPair.refreshToken());
        String sessionId = UUID.randomUUID().toString();

        sessionService.createSession(userId, sessionId, newAccessJti, newRefreshJti, 7 * 24 * 60 * 60);

        // Invalidate old refresh token
        sessionService.addToBlacklist(jti, jwtTokenProvider.getExpirationDate(refreshToken));

        // Build response
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .collect(Collectors.toSet());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                userId,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles
        );

        logger.info("Token refreshed for user: {}", user.getEmail());

        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer",
                tokenPair.expiresIn(),
                userInfo
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getCurrentUser(String token) {
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("Invalid token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<String> roles = user.getUserRoles().stream()
                .filter(ur -> ur.getRole().isActive())
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toSet());

        return new LoginResponse.UserInfo(
                userId,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles
        );
    }
}
