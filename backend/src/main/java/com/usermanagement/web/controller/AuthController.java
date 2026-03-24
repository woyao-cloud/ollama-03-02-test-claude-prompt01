package com.usermanagement.web.controller;

import com.usermanagement.service.AuthService;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.LoginRequest;
import com.usermanagement.web.dto.LoginResponse;
import com.usermanagement.web.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles login, logout, and token refresh endpoints
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        logger.info("Login request received for user: {}", loginRequest.getEmail());

        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponse response = authService.login(loginRequest, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("Logout request received");

        String token = extractTokenFromHeader(authHeader);
        authService.logout(token);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        logger.info("Token refresh request received");

        LoginResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("Get current user request received");

        String token = extractTokenFromHeader(authHeader);
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        LoginResponse.UserInfo userInfo = authService.getCurrentUser(token);

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * Extract token from Authorization header
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Extract client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle multiple IPs (e.g., "192.168.1.1, 10.0.0.1")
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
