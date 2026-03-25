package com.usermanagement.service;

import com.usermanagement.domain.entity.User;
import com.usermanagement.service.dto.CreateUserRequest;
import com.usermanagement.service.dto.UserDTO;
import com.usermanagement.service.dto.UserImportResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Excel Service Implementation Tests
 */
@ExtendWith(MockitoExtension.class)
class ExcelServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private ExcelServiceImpl excelService;

    private UserDTO sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new UserDTO();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("test@example.com");
        sampleUser.setFirstName("Test");
        sampleUser.setLastName("User");
        sampleUser.setPhone("+1234567890");
        sampleUser.setStatus(User.UserStatus.ACTIVE);
        sampleUser.setEmailVerified(true);
    }

    @Test
    @DisplayName("Should successfully import users from valid Excel file")
    void importUsers_Success() throws IOException {
        // Arrange
        Workbook workbook = createTestWorkbook(new String[][]{
                {"Email", "First Name", "Last Name", "Phone", "Status", "Department ID"},
                {"newuser1@example.com", "John", "Doe", "+1111111111", "ACTIVE", ""},
                {"newuser2@example.com", "Jane", "Smith", "+2222222222", "PENDING", ""}
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        when(userService.isEmailAvailable(anyString())).thenReturn(true);
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(sampleUser);

        // Act
        UserImportResult result = excelService.importUsers(file);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(userService, times(2)).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Should skip existing users during import")
    void importUsers_SkipExisting() throws IOException {
        // Arrange
        Workbook workbook = createTestWorkbook(new String[][]{
                {"Email", "First Name", "Last Name", "Phone", "Status", "Department ID"},
                {"existing@example.com", "John", "Doe", "", "ACTIVE", ""}
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        when(userService.isEmailAvailable("existing@example.com")).thenReturn(false);

        // Act
        UserImportResult result = excelService.importUsers(file);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Should report validation errors for invalid data")
    void importUsers_ValidationErrors() throws IOException {
        // Arrange - missing required fields
        Workbook workbook = createTestWorkbook(new String[][]{
                {"Email", "First Name", "Last Name", "Phone", "Status", "Department ID"},
                {"", "", "Doe", "", "", ""},
                {"invalid-email", "John", "", "", "INVALID_STATUS", ""}
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        // Act
        UserImportResult result = excelService.importUsers(file);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(2, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should validate import file without saving")
    void validateImport_Success() throws IOException {
        // Arrange
        Workbook workbook = createTestWorkbook(new String[][]{
                {"Email", "First Name", "Last Name", "Phone", "Status", "Department ID"},
                {"valid@example.com", "John", "Doe", "", "ACTIVE", ""}
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        // Act
        UserImportResult result = excelService.validateImport(file);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalCount());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should export users to Excel")
    void exportUsers_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(sampleUser);

        // Act
        ByteArrayInputStream result = excelService.exportUsers(List.of(userId));

        // Assert
        assertNotNull(result);

        // Verify the exported content
        try (Workbook workbook = new XSSFWorkbook(result)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Users", sheet.getSheetName());
            assertEquals(2, sheet.getLastRowNum() + 1); // Header + 1 data row

            Row headerRow = sheet.getRow(0);
            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Email", headerRow.getCell(1).getStringCellValue());

            Row dataRow = sheet.getRow(1);
            assertEquals(sampleUser.getEmail(), dataRow.getCell(1).getStringCellValue());
        } catch (IOException e) {
            fail("Failed to read exported Excel: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should export all users when no IDs specified")
    void exportUsers_AllUsers() {
        // Arrange
        Page<UserDTO> page = new PageImpl<>(List.of(sampleUser));
        when(userService.getUsers(any())).thenReturn(page);

        // Act
        ByteArrayInputStream result = excelService.exportUsers(null);

        // Assert
        assertNotNull(result);
        verify(userService).getUsers(any());
    }

    @Test
    @DisplayName("Should export users with filters")
    void exportUsersWithFilter_Success() {
        // Arrange
        Map<String, String> filters = Map.of("status", "ACTIVE");
        Pageable pageable = PageRequest.of(0, 20);

        Page<UserDTO> page = new PageImpl<>(List.of(sampleUser));
        when(userService.getUsers(any())).thenReturn(page);

        // Act
        ByteArrayInputStream result = excelService.exportUsersWithFilter(filters, pageable);

        // Assert
        assertNotNull(result);
        verify(userService).getUsers(any());
    }

    @Test
    @DisplayName("Should generate import template")
    void exportTemplate_Success() {
        // Act
        ByteArrayInputStream result = excelService.exportTemplate();

        // Assert
        assertNotNull(result);

        try (Workbook workbook = new XSSFWorkbook(result)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Template", sheet.getSheetName());

            Row headerRow = sheet.getRow(0);
            assertEquals("Email (Required)", headerRow.getCell(0).getStringCellValue());
            assertEquals("First Name (Required)", headerRow.getCell(1).getStringCellValue());

            // Check sample data row exists
            Row sampleRow = sheet.getRow(1);
            assertNotNull(sampleRow);
            assertEquals("john.doe@example.com", sampleRow.getCell(0).getStringCellValue());
        } catch (IOException e) {
            fail("Failed to read template Excel: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should return export fields map")
    void getExportFields_Success() {
        // Act
        Map<String, String> fields = excelService.getExportFields();

        // Assert
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        assertTrue(fields.containsKey("email"));
        assertTrue(fields.containsKey("firstName"));
        assertTrue(fields.containsKey("lastName"));
        assertEquals("Email", fields.get("email"));
    }

    @Test
    @DisplayName("Should preview import with limit")
    void previewImport_Success() throws IOException {
        // Arrange
        Workbook workbook = createTestWorkbook(new String[][]{
                {"Email", "First Name", "Last Name", "Phone", "Status", "Department ID"},
                {"user1@example.com", "John", "Doe", "", "ACTIVE", ""},
                {"user2@example.com", "Jane", "Smith", "", "PENDING", ""},
                {"user3@example.com", "Bob", "Wilson", "", "ACTIVE", ""}
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        // Act - limit to 2 rows
        UserImportResult result = excelService.previewImport(file, 2);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(2, result.getPreview().size());
        assertEquals("user1@example.com", result.getPreview().get(0).getEmail());
        assertEquals("user2@example.com", result.getPreview().get(1).getEmail());
    }

    @Test
    @DisplayName("Should handle empty Excel file")
    void importUsers_EmptyFile() throws IOException {
        // Arrange - only header row
        Workbook workbook = createTestWorkbook(new String[][]{
                {"Email", "First Name", "Last Name", "Phone", "Status", "Department ID"}
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        // Act
        UserImportResult result = excelService.importUsers(file);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
    }

    @Test
    @DisplayName("Should handle file with blank rows")
    void importUsers_WithBlankRows() throws IOException {
        // Arrange - file with gaps
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Email");
        header.createCell(1).setCellValue("First Name");

        // Row 1 is null (blank)
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("valid@example.com");
        row2.createCell(1).setCellValue("John");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "test.xlsx", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        when(userService.isEmailAvailable(anyString())).thenReturn(true);
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(sampleUser);

        // Act
        UserImportResult result = excelService.importUsers(file);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(1, result.getSuccessCount());
    }

    @Test
    @DisplayName("Should export users with selected fields - fallback to all fields")
    void exportUsersWithFields_FallbackToAll() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(sampleUser);

        // Act - fields parameter is ignored in current implementation
        ByteArrayInputStream result = excelService.exportUsersWithFields(
                List.of(userId), List.of("email", "firstName"));

        // Assert
        assertNotNull(result);
        verify(userService).getUserById(userId);
    }

    // Helper method
    private Workbook createTestWorkbook(String[][] data) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users");

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue(data[i][j]);
            }
        }

        return workbook;
    }
}
