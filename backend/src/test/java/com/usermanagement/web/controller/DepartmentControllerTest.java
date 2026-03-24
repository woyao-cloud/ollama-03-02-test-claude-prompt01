package com.usermanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.service.DepartmentService;
import com.usermanagement.service.dto.CreateDepartmentRequest;
import com.usermanagement.service.dto.DepartmentDTO;
import com.usermanagement.service.dto.UpdateDepartmentRequest;
import com.usermanagement.web.dto.AssignManagerRequest;
import com.usermanagement.web.dto.MoveDepartmentRequest;
import com.usermanagement.web.dto.UpdateDepartmentStatusRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Department Controller Test
 * Integration tests for Department REST API endpoints
 *
 * @author Test Team
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepartmentService departmentService;

    private UUID testDeptId;
    private DepartmentDTO testDepartment;

    @BeforeEach
    void setUp() {
        testDeptId = UUID.randomUUID();
        testDepartment = createTestDepartmentDTO(testDeptId, "IT Department", "IT", null);
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldGetDepartments() throws Exception {
        // Given
        List<DepartmentDTO> departments = Arrays.asList(
                testDepartment,
                createTestDepartmentDTO(UUID.randomUUID(), "HR", "HR", null)
        );
        Page<DepartmentDTO> page = new PageImpl<>(departments);

        when(departmentService.getDepartments(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/departments")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].name").value("IT Department"));
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldGetDepartmentById() throws Exception {
        // Given
        when(departmentService.getDepartmentById(testDeptId)).thenReturn(testDepartment);

        // When & Then
        mockMvc.perform(get("/api/v1/departments/{id}", testDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testDeptId.toString()))
                .andExpect(jsonPath("$.data.name").value("IT Department"))
                .andExpect(jsonPath("$.data.code").value("IT"));
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldGetDepartmentByCode() throws Exception {
        // Given
        when(departmentService.getDepartmentByCode("IT")).thenReturn(testDepartment);

        // When & Then
        mockMvc.perform(get("/api/v1/departments/code/{code}", "IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("IT"));
    }

    @Test
    @WithMockUser(authorities = "DEPT_CREATE")
    void shouldCreateDepartment() throws Exception {
        // Given
        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("New Department");
        request.setCode("NEW");
        request.setDescription("A new department");

        DepartmentDTO created = createTestDepartmentDTO(UUID.randomUUID(), "New Department", "NEW", null);

        when(departmentService.createDepartment(any())).thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Department"));
    }

    @Test
    @WithMockUser(authorities = "DEPT_CREATE")
    void shouldReturnBadRequestWhenCreatingDepartmentWithInvalidData() throws Exception {
        // Given - Invalid request (empty name)
        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setName("");
        request.setCode("NEW");

        // When & Then
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "DEPT_UPDATE")
    void shouldUpdateDepartment() throws Exception {
        // Given
        UpdateDepartmentRequest request = new UpdateDepartmentRequest();
        request.setName("Updated Department");
        request.setDescription("Updated description");

        DepartmentDTO updated = createTestDepartmentDTO(testDeptId, "Updated Department", "IT", null);

        when(departmentService.updateDepartment(eq(testDeptId), any())).thenReturn(updated);

        // When & Then
        mockMvc.perform(put("/api/v1/departments/{id}", testDeptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Department"));
    }

    @Test
    @WithMockUser(authorities = "DEPT_UPDATE")
    void shouldUpdateDepartmentStatus() throws Exception {
        // Given
        UpdateDepartmentStatusRequest request = new UpdateDepartmentStatusRequest("INACTIVE");

        // When & Then
        mockMvc.perform(patch("/api/v1/departments/{id}/status", testDeptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "DEPT_DELETE")
    void shouldDeleteDepartment() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/departments/{id}", testDeptId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldGetDepartmentTree() throws Exception {
        // Given
        List<DepartmentDTO> tree = Arrays.asList(
                createTestDepartmentDTO(UUID.randomUUID(), "Root", "ROOT", null),
                createTestDepartmentDTO(UUID.randomUUID(), "Another Root", "ROOT2", null)
        );

        when(departmentService.getDepartmentTree(false)).thenReturn(tree);

        // When & Then
        mockMvc.perform(get("/api/v1/departments/tree")
                        .param("includeInactive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldGetChildren() throws Exception {
        // Given
        List<DepartmentDTO> children = Arrays.asList(
                createTestDepartmentDTO(UUID.randomUUID(), "Child 1", "CHILD1", null),
                createTestDepartmentDTO(UUID.randomUUID(), "Child 2", "CHILD2", null)
        );

        when(departmentService.getChildren(testDeptId, false)).thenReturn(children);

        // When & Then
        mockMvc.perform(get("/api/v1/departments/{id}/children", testDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @WithMockUser(authorities = "DEPT_UPDATE")
    void shouldMoveDepartment() throws Exception {
        // Given
        UUID newParentId = UUID.randomUUID();
        MoveDepartmentRequest request = new MoveDepartmentRequest(newParentId);

        // When & Then
        mockMvc.perform(post("/api/v1/departments/{id}/move", testDeptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "DEPT_UPDATE")
    void shouldAssignManager() throws Exception {
        // Given
        UUID managerId = UUID.randomUUID();
        AssignManagerRequest request = new AssignManagerRequest(managerId);

        // When & Then
        mockMvc.perform(post("/api/v1/departments/{id}/manager", testDeptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldGetDescendantIds() throws Exception {
        // Given
        List<UUID> descendantIds = Arrays.asList(
                testDeptId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(departmentService.getDescendantIds(testDeptId)).thenReturn(descendantIds);

        // When & Then
        mockMvc.perform(get("/api/v1/departments/{id}/descendants", testDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @WithMockUser(authorities = "DEPT_READ")
    void shouldCheckCodeAvailability() throws Exception {
        // Given
        when(departmentService.isCodeAvailable("AVAILABLE", null)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/departments/check-code")
                        .param("code", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void shouldReturnForbiddenForUnauthorizedAccess() throws Exception {
        // When & Then - No authentication
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void shouldReturnForbiddenForInsufficientPermissions() throws Exception {
        // When & Then - Wrong authority
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isForbidden());
    }

    // Helper method
    private DepartmentDTO createTestDepartmentDTO(UUID id, String name, String code, UUID parentId) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCode(code);
        dto.setParentId(parentId);
        dto.setLevel(1);
        dto.setPath("/" + id);
        dto.setSortOrder(0);
        dto.setStatus(com.usermanagement.domain.entity.Department.DepartmentStatus.ACTIVE);
        dto.setUserCount(0L);
        dto.setCreatedAt(new Date().toInstant());
        dto.setUpdatedAt(new Date().toInstant());
        return dto;
    }
}
