package com.usermanagement.service;

import com.usermanagement.domain.entity.PasswordHistory;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.PasswordHistoryRepository;
import com.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Password Policy Service Test
 *
 * @author Test Team
 * @since 1.0
 */
class PasswordPolicyServiceImplTest {

    private PasswordPolicyService passwordPolicyService;
    private PasswordHistoryRepository passwordHistoryRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordHistoryRepository = mock(PasswordHistoryRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(12);

        passwordPolicyService = new PasswordPolicyServiceImpl(
                8,      // minLength
                true,   // requireUppercase
                true,   // requireLowercase
                true,   // requireDigit
                true,   // requireSpecialChar
                90,     // expiryDays
                12,     // historyCount
                passwordHistoryRepository,
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldValidateValidPassword() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("Test123!@");

        assertTrue(result.isValid());
    }

    @Test
    void shouldRejectShortPassword() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("Test1!");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("at least 8 characters"));
    }

    @Test
    void shouldRejectPasswordWithoutUppercase() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("test123!@");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("uppercase"));
    }

    @Test
    void shouldRejectPasswordWithoutLowercase() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("TEST123!@");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("lowercase"));
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("TestPass!@");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("digit"));
    }

    @Test
    void shouldRejectPasswordWithoutSpecialChar() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("TestPass12");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("special character"));
    }

    @Test
    void shouldRejectPasswordContainingUsername() {
        PasswordPolicyService.ValidationResult result =
                passwordPolicyService.validatePassword("TestUser123!", "testuser");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("username"));
    }

    @Test
    void shouldCheckPasswordHistory() {
        UUID userId = UUID.randomUUID();
        String password = "Test123!@";
        String hashedPassword = passwordEncoder.encode(password);

        PasswordHistory history = new PasswordHistory();
        history.setPasswordHash(hashedPassword);

        when(passwordHistoryRepository.findRecentByUserId(userId, 12))
                .thenReturn(Collections.singletonList(history));

        boolean exists = passwordPolicyService.checkPasswordHistory(userId, password);

        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseForNewPassword() {
        UUID userId = UUID.randomUUID();
        String oldPassword = "OldPass123!";
        String newPassword = "NewPass123!";

        PasswordHistory history = new PasswordHistory();
        history.setPasswordHash(passwordEncoder.encode(oldPassword));

        when(passwordHistoryRepository.findRecentByUserId(userId, 12))
                .thenReturn(Collections.singletonList(history));

        boolean exists = passwordPolicyService.checkPasswordHistory(userId, newPassword);

        assertFalse(exists);
    }

    @Test
    void shouldCheckPasswordExpired() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setPasswordChangedAt(Instant.now().minusSeconds(91L * 24 * 60 * 60)); // 91 days ago

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean expired = passwordPolicyService.isPasswordExpired(userId);

        assertTrue(expired);
    }

    @Test
    void shouldCheckPasswordNotExpired() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setPasswordChangedAt(Instant.now().minusSeconds(30L * 24 * 60 * 60)); // 30 days ago

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean expired = passwordPolicyService.isPasswordExpired(userId);

        assertFalse(expired);
    }

    @Test
    void shouldAddPasswordToHistory() {
        UUID userId = UUID.randomUUID();
        String hashedPassword = passwordEncoder.encode("Test123!@");

        when(passwordHistoryRepository.findRecentByUserId(userId, 13))
                .thenReturn(Collections.emptyList());

        passwordPolicyService.addPasswordToHistory(userId, hashedPassword);

        verify(passwordHistoryRepository).save(any(PasswordHistory.class));
    }
}
