package com.usermanagement.web.controller;

import com.usermanagement.service.ExcelService;
import com.usermanagement.service.dto.UserImportResult;
import com.usermanagement.web.dto.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Excel Import/Export Controller
 * REST API endpoints for user batch import and export operations
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
public class ExcelController {

    private static final Logger logger = LoggerFactory.getLogger(ExcelController.class);

    private final ExcelService excelService;

    @Autowired
    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }

    /**
     * Import users from Excel file
     *
     * @param file Excel file to import
     * @return Import result with success/failure details
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserImportResult>> importUsers(
            @RequestParam("file") MultipartFile file) {

        logger.info("Processing user import from file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please upload a file"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please upload an Excel file (.xlsx or .xls)"));
        }

        try {
            UserImportResult result = excelService.importUsers(file);

            if (result.isSuccess() && result.getFailureCount() == 0) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Import completed successfully", result));
            } else if (result.getSuccessCount() > 0) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Import completed with some errors", result));
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("Import failed", result));
            }
        } catch (IOException e) {
            logger.error("Error processing import file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process file: " + e.getMessage()));
        }
    }

    /**
     * Validate import file without actually importing
     *
     * @param file Excel file to validate
     * @return Validation result with errors if any
     */
    @PostMapping(value = "/import/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserImportResult>> validateImport(
            @RequestParam("file") MultipartFile file) {

        logger.info("Validating import file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please upload a file"));
        }

        try {
            UserImportResult result = excelService.validateImport(file);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Validation passed - file is ready for import", result));
            } else {
                return ResponseEntity.ok(ApiResponse.success(
                        "Validation failed - please fix errors before importing", result));
            }
        } catch (IOException e) {
            logger.error("Error validating import file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to validate file: " + e.getMessage()));
        }
    }

    /**
     * Preview import data - show first N rows without saving
     *
     * @param file Excel file to preview
     * @param limit Maximum number of rows to preview (default: 10)
     * @return Preview data with validation results
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserImportResult>> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("Previewing import file: {}, limit: {}", file.getOriginalFilename(), limit);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please upload a file"));
        }

        // Validate limit
        if (limit < 1 || limit > 100) {
            limit = 10; // Reset to default if out of range
        }

        try {
            UserImportResult result = excelService.previewImport(file, limit);
            return ResponseEntity.ok(ApiResponse.success(
                    "Preview generated with " + result.getPreview().size() + " rows", result));
        } catch (IOException e) {
            logger.error("Error previewing import file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to preview file: " + e.getMessage()));
        }
    }

    /**
     * Export users to Excel
     *
     * @param userIds Optional list of user IDs to export (if empty, exports all)
     * @return Excel file as download
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('USER_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) List<UUID> userIds) {

        logger.info("Exporting users, count: {}", userIds != null ? userIds.size() : "all");

        ByteArrayInputStream stream = excelService.exportUsers(userIds);

        try {
            byte[] content = stream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "users_export.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            logger.error("Error reading export stream", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export users with filters
     *
     * @param filters Map of filter criteria
     * @return Excel file as download
     */
    @PostMapping("/export/filtered")
    @PreAuthorize("hasAuthority('USER_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportUsersWithFilter(
            @RequestBody Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        logger.info("Exporting users with filters: {}", filters);

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        ByteArrayInputStream stream = excelService.exportUsersWithFilter(filters, pageable);

        try {
            byte[] content = stream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "users_export_filtered.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            logger.error("Error reading export stream", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download import template
     *
     * @return Excel template file as download
     */
    @GetMapping("/export/template")
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {

        logger.info("Downloading import template");

        ByteArrayInputStream stream = excelService.exportTemplate();

        try {
            byte[] content = stream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "user_import_template.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            logger.error("Error reading template stream", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get supported export fields
     *
     * @return Map of field names to display names
     */
    @GetMapping("/export/fields")
    @PreAuthorize("hasAuthority('USER_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getExportFields() {

        logger.debug("Getting export fields");

        Map<String, String> fields = excelService.getExportFields();
        return ResponseEntity.ok(ApiResponse.success(
                "Export fields retrieved successfully", fields));
    }
}
