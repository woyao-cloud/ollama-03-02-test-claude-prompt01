package com.usermanagement.service;

import com.usermanagement.domain.entity.PasswordHistory;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.PasswordHistoryRepository;
import com.usermanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Password Policy Service Implementation
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicyServiceImpl.class);

    // Password policy configuration
    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecialChar;
    private final int expiryDays;
    private final int historyCount;

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyServiceImpl(
            @Value("${password.policy.min-length:8}") int minLength,
            @Value("${password.policy.require-uppercase:true}") boolean requireUppercase,
            @Value("${password.policy.require-lowercase:true}") boolean requireLowercase,
            @Value("${password.policy.require-digit:true}") boolean requireDigit,
            @Value("${password.policy.require-special-char:true}") boolean requireSpecialChar,
            @Value("${password.policy.expiry-days:90}") int expiryDays,
            @Value("${password.policy.history-count:12}") int historyCount,
            PasswordHistoryRepository passwordHistoryRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.minLength = minLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecialChar = requireSpecialChar;
        this.expiryDays = expiryDays;
        this.historyCount = historyCount;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ValidationResult validatePassword(String password) {
        return validatePassword(password, null);
    }

    @Override
    public ValidationResult validatePassword(String password, String username) {
        // Check minimum length
        if (password.length() < minLength) {
            return ValidationResult.invalid(
                    String.format("Password must be at least %d characters long", minLength));
        }

        // Check uppercase
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            return ValidationResult.invalid("Password must contain at least one uppercase letter");
        }

        // Check lowercase
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            return ValidationResult.invalid("Password must contain at least one lowercase letter");
        }

        // Check digit
        if (requireDigit && !password.matches(".*\\d.*")) {
            return ValidationResult.invalid("Password must contain at least one digit");
        }

        // Check special character
        if (requireSpecialChar && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return ValidationResult.invalid("Password must contain at least one special character");
        }

        // Check username (if provided)
        if (username != null && !username.isEmpty()) {
            String lowerPassword = password.toLowerCase();
            String lowerUsername = username.toLowerCase();

            if (lowerPassword.contains(lowerUsername)) {
                return ValidationResult.invalid("Password must not contain the username");
            }
        }

        return ValidationResult.valid();
    }

    @Override
    public boolean isPasswordExpired(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        Instant passwordChangedAt = user.getPasswordChangedAt();
        if (passwordChangedAt == null) {
            // If never changed, use created_at (handled in entity)
            passwordChangedAt = user.getCreatedAt();
        }

        Instant expiryDate = passwordChangedAt.plus(expiryDays, ChronoUnit.DAYS);
        return Instant.now().isAfter(expiryDate);
    }

    @Override
    public boolean checkPasswordHistory(UUID userId, String password) {
        List<PasswordHistory> history = passwordHistoryRepository
                .findRecentByUserId(userId, historyCount);

        for (PasswordHistory entry : history) {
            if (passwordEncoder.matches(password, entry.getPasswordHash())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void addPasswordToHistory(UUID userId, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        history.setCreatedAt(Instant.now());

        passwordHistoryRepository.save(history);

        // Clean up old entries if exceeding history count
        List<PasswordHistory> allHistory = passwordHistoryRepository
                .findRecentByUserId(userId, historyCount + 1);

        if (allHistory.size() > historyCount) {
            for (int i = historyCount; i < allHistory.size(); i++) {
                passwordHistoryRepository.delete(allHistory.get(i));
            }
        }

        logger.debug("Added password to history for user: {}", userId);
    }
}
