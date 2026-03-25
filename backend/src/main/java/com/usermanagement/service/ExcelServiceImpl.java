package com.usermanagement.service;

import com.usermanagement.domain.entity.User;
import com.usermanagement.service.dto.CreateUserRequest;
import com.usermanagement.service.dto.UserDTO;
import com.usermanagement.service.dto.UserImportResult;
import com.usermanagement.service.dto.UserQueryRequest;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel Service Implementation
 * Handles Excel import and export operations using Apache POI
 *
 * @author Service Team
 * @since 1.0
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelServiceImpl.class);
    private static final String SHEET_NAME = "Users";
    private static final String TEMPLATE_SHEET_NAME = "Template";
    private static final int MAX_IMPORT_ROWS = 10000;

    private final UserService userService;

    public ExcelServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional
    public UserImportResult importUsers(MultipartFile file) throws IOException {
        logger.info("Starting user import from file: {}", file.getOriginalFilename());

        UserImportResult result = new UserImportResult();
        List<UserImportResult.ImportError> errors = new ArrayList<>();
        List<UserDTO> importedUsers = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            result.setTotalCount(sheet.getLastRowNum()); // Excluding header

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    UserImportResult.ImportRow importRow = parseRow(row);
                    List<String> validationErrors = validateRow(importRow);

                    if (validationErrors.isEmpty()) {
                        UserDTO user = createUserFromImport(importRow);
                        if (user != null) {
                            importedUsers.add(user);
                            result.incrementSuccess();
                        } else {
                            result.incrementSkipped();
                        }
                    } else {
                        for (String error : validationErrors) {
                            errors.add(new UserImportResult.ImportError(
                                    i + 1, "multiple", importRow.getEmail(), error));
                        }
                        result.incrementFailure();
                    }
                } catch (Exception e) {
                    logger.error("Error processing row {}: {}", i + 1, e.getMessage());
                    errors.add(new UserImportResult.ImportError(
                            i + 1, "unknown", "", "Processing error: " + e.getMessage()));
                    result.incrementFailure();
                }
            }
        }

        result.setErrors(errors);
        result.setImportedUsers(importedUsers);
        result.setSuccess(result.getFailureCount() == 0);
        result.setMessage(String.format("Import completed: %d success, %d failed, %d skipped",
                result.getSuccessCount(), result.getFailureCount(), result.getSkippedCount()));

        logger.info("User import completed: {}", result.getMessage());
        return result;
    }

    @Override
    public UserImportResult validateImport(MultipartFile file) throws IOException {
        logger.info("Validating import file: {}", file.getOriginalFilename());

        UserImportResult result = new UserImportResult();
        List<UserImportResult.ImportError> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            result.setTotalCount(sheet.getLastRowNum());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                UserImportResult.ImportRow importRow = parseRow(row);
                List<String> validationErrors = validateRow(importRow);

                if (!validationErrors.isEmpty()) {
                    for (String error : validationErrors) {
                        errors.add(new UserImportResult.ImportError(
                                i + 1, "validation", importRow.getEmail(), error));
                    }
                }
            }
        }

        result.setErrors(errors);
        result.setSuccess(errors.isEmpty());
        result.setMessage(errors.isEmpty() ? "Validation passed" : "Validation failed with " + errors.size() + " errors");

        return result;
    }

    @Override
    public ByteArrayInputStream exportUsers(List<UUID> userIds) {
        logger.info("Exporting users, count: {}", userIds != null ? userIds.size() : "all");

        List<UserDTO> users;
        if (userIds != null && !userIds.isEmpty()) {
            users = new ArrayList<>();
            for (UUID id : userIds) {
                try {
                    users.add(userService.getUserById(id));
                } catch (Exception e) {
                    logger.warn("User not found for export: {}", id);
                }
            }
        } else {
            UserQueryRequest query = new UserQueryRequest();
            query.setPage(0);
            query.setSize(10000);
            Page<UserDTO> page = userService.getUsers(query);
            users = page.getContent();
        }

        return exportToExcel(users);
    }

    @Override
    public ByteArrayInputStream exportUsersWithFilter(Map<String, String> filters, Pageable pageable) {
        logger.info("Exporting users with filters: {}", filters);

        UserQueryRequest query = new UserQueryRequest();
        query.setPage(pageable.getPageNumber());
        query.setSize(pageable.getPageSize());

        // Apply filters
        if (filters.containsKey("status")) {
            try {
                query.setStatus(User.UserStatus.valueOf(filters.get("status").toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", filters.get("status"));
            }
        }

        Page<UserDTO> page = userService.getUsers(query);
        return exportToExcel(page.getContent());
    }

    @Override
    public ByteArrayInputStream exportTemplate() {
        logger.info("Generating import template");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(TEMPLATE_SHEET_NAME);

            // Create header row with required fields
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Email (Required)",
                    "First Name (Required)",
                    "Last Name (Required)",
                    "Phone",
                    "Status (ACTIVE/PENDING/LOCKED/INACTIVE)",
                    "Department ID"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add sample data row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("john.doe@example.com");
            sampleRow.createCell(1).setCellValue("John");
            sampleRow.createCell(2).setCellValue("Doe");
            sampleRow.createCell(3).setCellValue("+1234567890");
            sampleRow.createCell(4).setCellValue("PENDING");

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            logger.error("Error generating template", e);
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    @Override
    public ByteArrayInputStream exportUsersWithFields(List<UUID> userIds, List<String> fields) {
        // For simplicity, export all fields if specific fields not supported yet
        return exportUsers(userIds);
    }

    @Override
    public Map<String, String> getExportFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "User ID");
        fields.put("email", "Email");
        fields.put("firstName", "First Name");
        fields.put("lastName", "Last Name");
        fields.put("fullName", "Full Name");
        fields.put("phone", "Phone");
        fields.put("departmentName", "Department");
        fields.put("status", "Status");
        fields.put("emailVerified", "Email Verified");
        fields.put("lastLoginAt", "Last Login");
        fields.put("createdAt", "Created At");
        fields.put("roles", "Roles");
        return fields;
    }

    @Override
    public UserImportResult previewImport(MultipartFile file, int limit) throws IOException {
        logger.info("Previewing import, limit: {}", limit);

        UserImportResult result = new UserImportResult();
        List<UserImportResult.ImportRow> preview = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= Math.min(sheet.getLastRowNum(), limit); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                UserImportResult.ImportRow importRow = parseRow(row);
                List<String> validationErrors = validateRow(importRow);
                importRow.setValidationErrors(validationErrors);
                importRow.setRowNumber(i + 1);

                preview.add(importRow);
            }
        }

        result.setPreview(preview);
        result.setTotalCount(preview.size());
        result.setSuccess(true);
        result.setMessage("Preview generated with " + preview.size() + " rows");

        return result;
    }

    // Helper methods

    private UserImportResult.ImportRow parseRow(Row row) {
        UserImportResult.ImportRow importRow = new UserImportResult.ImportRow();
        importRow.setEmail(getCellValue(row.getCell(0)));
        importRow.setFirstName(getCellValue(row.getCell(1)));
        importRow.setLastName(getCellValue(row.getCell(2)));
        importRow.setStatus(getCellValue(row.getCell(4)));
        return importRow;
    }

    private List<String> validateRow(UserImportResult.ImportRow row) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(row.getEmail())) {
            errors.add("Email is required");
        } else if (!isValidEmail(row.getEmail())) {
            errors.add("Invalid email format");
        }

        if (!StringUtils.hasText(row.getFirstName())) {
            errors.add("First name is required");
        }

        if (!StringUtils.hasText(row.getLastName())) {
            errors.add("Last name is required");
        }

        if (StringUtils.hasText(row.getStatus())) {
            try {
                User.UserStatus.valueOf(row.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid status. Must be: ACTIVE, PENDING, LOCKED, or INACTIVE");
            }
        }

        return errors;
    }

    private UserDTO createUserFromImport(UserImportResult.ImportRow row) {
        try {
            // Check if user already exists
            if (!userService.isEmailAvailable(row.getEmail())) {
                logger.warn("User already exists with email: {}", row.getEmail());
                return null;
            }

            CreateUserRequest request = new CreateUserRequest();
            request.setEmail(row.getEmail());
            request.setFirstName(row.getFirstName());
            request.setLastName(row.getLastName());

            // Generate random password
            request.setPassword(generateRandomPassword());

            UserDTO user = userService.createUser(request);

            // Set status if specified
            if (StringUtils.hasText(row.getStatus())) {
                userService.updateStatus(user.getId(), row.getStatus());
            }

            return user;

        } catch (Exception e) {
            logger.error("Error creating user from import: {}", e.getMessage());
            return null;
        }
    }

    private ByteArrayInputStream exportToExcel(List<UserDTO> users) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // Create header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Email", "First Name", "Last Name", "Phone",
                    "Department", "Status", "Email Verified", "Last Login", "Created At"};

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowNum = 1;

            for (UserDTO user : users) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId().toString());
                row.createCell(1).setCellValue(user.getEmail());
                row.createCell(2).setCellValue(user.getFirstName());
                row.createCell(3).setCellValue(user.getLastName());
                row.createCell(4).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                row.createCell(5).setCellValue(user.getDepartmentName() != null ? user.getDepartmentName() : "");
                row.createCell(6).setCellValue(user.getStatus() != null ? user.getStatus().name() : "");
                row.createCell(7).setCellValue(user.getEmailVerified() != null && user.getEmailVerified() ? "Yes" : "No");
                row.createCell(8).setCellValue(user.getLastLoginAt() != null ?
                        formatter.format(user.getLastLoginAt()) : "");
                row.createCell(9).setCellValue(user.getCreatedAt() != null ?
                        formatter.format(user.getCreatedAt()) : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            logger.error("Error exporting to Excel", e);
            throw new RuntimeException("Failed to export users", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private String generateRandomPassword() {
        // Generate a random 12-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
