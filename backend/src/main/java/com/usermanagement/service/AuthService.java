package com.usermanagement.service;

import com.usermanagement.web.dto.LoginRequest;
import com.usermanagement.web.dto.LoginResponse;
import com.usermanagement.web.dto.RefreshTokenRequest;

/**
 * Authentication Service Interface
 * Handles user authentication, token management, and session control
 *
 * @author Service Team
 * @since 1.0
 */
public interface AuthService {

    /**
     * Authenticate user with email and password
     *
     * @param loginRequest login credentials
     * @param ipAddress client IP address
     * @param userAgent client user agent
     * @return login response with tokens
     */
    LoginResponse login(LoginRequest loginRequest, String ipAddress, String userAgent);

    /**
     * Logout user and invalidate token
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * Refresh access token using refresh token
     *
     * @param refreshTokenRequest refresh token request
     * @return new login response with tokens
     */
    LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    /**
     * Get current authenticated user info
     *
     * @param token JWT token
     * @return user info
     */
    LoginResponse.UserInfo getCurrentUser(String token);
}
