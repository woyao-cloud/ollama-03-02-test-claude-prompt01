package com.usermanagement.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * User Import Result
 * Result of user import operation
 *
 * @author Service Team
 * @since 1.0
 */
public class UserImportResult {

    private int totalCount;
    private int successCount;
    private int failureCount;
    private int skippedCount;
    private boolean success;
    private String message;
    private List<ImportError> errors = new ArrayList<>();
    private List<UserDTO> importedUsers = new ArrayList<>();
    private List<ImportRow> preview = new ArrayList<>();

    // Inner class for import errors
    public static class ImportError {
        private int rowNumber;
        private String field;
        private String value;
        private String errorMessage;

        public ImportError() {}

        public ImportError(int rowNumber, String field, String value, String errorMessage) {
            this.rowNumber = rowNumber;
            this.field = field;
            this.value = value;
            this.errorMessage = errorMessage;
        }

        // Getters and Setters
        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    // Inner class for preview rows
    public static class ImportRow {
        private int rowNumber;
        private String email;
        private String firstName;
        private String lastName;
        private String status;
        private List<String> validationErrors = new ArrayList<>();

        // Getters and Setters
        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(List<String> validationErrors) {
            this.validationErrors = validationErrors;
        }

        public boolean isValid() {
            return validationErrors.isEmpty();
        }
    }

    // Constructors
    public UserImportResult() {}

    public static UserImportResult success(int totalCount, int successCount, List<UserDTO> importedUsers) {
        UserImportResult result = new UserImportResult();
        result.setSuccess(true);
        result.setTotalCount(totalCount);
        result.setSuccessCount(successCount);
        result.setFailureCount(0);
        result.setMessage("Import completed successfully");
        result.setImportedUsers(importedUsers);
        return result;
    }

    public static UserImportResult partial(int totalCount, int successCount, int failureCount, List<ImportError> errors) {
        UserImportResult result = new UserImportResult();
        result.setSuccess(failureCount == 0);
        result.setTotalCount(totalCount);
        result.setSuccessCount(successCount);
        result.setFailureCount(failureCount);
        result.setMessage(failureCount == 0 ? "Import completed" : "Import completed with errors");
        result.setErrors(errors);
        return result;
    }

    public static UserImportResult failure(String message, List<ImportError> errors) {
        UserImportResult result = new UserImportResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setErrors(errors);
        return result;
    }

    // Getters and Setters
    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ImportError> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportError> errors) {
        this.errors = errors;
    }

    public List<UserDTO> getImportedUsers() {
        return importedUsers;
    }

    public void setImportedUsers(List<UserDTO> importedUsers) {
        this.importedUsers = importedUsers;
    }

    public List<ImportRow> getPreview() {
        return preview;
    }

    public void setPreview(List<ImportRow> preview) {
        this.preview = preview;
    }

    /**
     * Add an error
     */
    public void addError(int rowNumber, String field, String value, String errorMessage) {
        errors.add(new ImportError(rowNumber, field, value, errorMessage));
    }

    /**
     * Increment success count
     */
    public void incrementSuccess() {
        this.successCount++;
    }

    /**
     * Increment failure count
     */
    public void incrementFailure() {
        this.failureCount++;
    }

    /**
     * Increment skipped count
     */
    public void incrementSkipped() {
        this.skippedCount++;
    }
}
