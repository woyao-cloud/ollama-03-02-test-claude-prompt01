package com.usermanagement.service;

import com.usermanagement.service.dto.UserImportResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Excel Service Interface
 * Handles Excel import and export operations
 *
 * @author Service Team
 * @since 1.0
 */
public interface ExcelService {

    /**
     * Import users from Excel file
     *
     * @param file Excel file
     * @return import result with success/failure details
     * @throws IOException if file reading fails
     */
    UserImportResult importUsers(MultipartFile file) throws IOException;

    /**
     * Validate import template without importing
     *
     * @param file Excel file
     * @return validation result with errors if any
     * @throws IOException if file reading fails
     */
    UserImportResult validateImport(MultipartFile file) throws IOException;

    /**
     * Export users to Excel
     *
     * @param userIds list of user IDs to export (null for all)
     * @return Excel file as input stream
     */
    ByteArrayInputStream exportUsers(List<UUID> userIds);

    /**
     * Export users with filters
     *
     * @param filters export filters
     * @param pageable pagination
     * @return Excel file as input stream
     */
    ByteArrayInputStream exportUsersWithFilter(Map<String, String> filters, Pageable pageable);

    /**
     * Export users template for import
     *
     * @return template Excel file as input stream
     */
    ByteArrayInputStream exportTemplate();

    /**
     * Export users with selected fields
     *
     * @param userIds user IDs to export
     * @param fields fields to include
     * @return Excel file as input stream
     */
    ByteArrayInputStream exportUsersWithFields(List<UUID> userIds, List<String> fields);

    /**
     * Get supported export fields
     *
     * @return list of field names and descriptions
     */
    Map<String, String> getExportFields();

    /**
     * Import preview - show first N rows without saving
     *
     * @param file Excel file
     * @param limit max rows to preview
     * @return preview data
     * @throws IOException if file reading fails
     */
    UserImportResult previewImport(MultipartFile file, int limit) throws IOException;
}
