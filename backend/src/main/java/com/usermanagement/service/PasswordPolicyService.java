package com.usermanagement.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Password Policy Service Interface
 * Handles password complexity validation and password history
 *
 * @author Service Team
 * @since 1.0
 */
public interface PasswordPolicyService {

    /**
     * Validate password complexity
     *
     * @param password password to validate
     * @return validation result
     */
    ValidationResult validatePassword(String password);

    /**
     * Validate password complexity with username check
     *
     * @param password password to validate
     * @param username username to check against
     * @return validation result
     */
    ValidationResult validatePassword(String password, String username);

    /**
     * Check if password is expired
     *
     * @param userId user ID
     * @return true if expired
     */
    boolean isPasswordExpired(UUID userId);

    /**
     * Check if password exists in history
     *
     * @param userId user ID
     * @param password password to check
     * @return true if in history
     */
    boolean checkPasswordHistory(UUID userId, String password);

    /**
     * Add password to history
     *
     * @param userId user ID
     * @param passwordHash hashed password
     */
    void addPasswordToHistory(UUID userId, String passwordHash);

    /**
     * Validation result class
     */
    class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}
