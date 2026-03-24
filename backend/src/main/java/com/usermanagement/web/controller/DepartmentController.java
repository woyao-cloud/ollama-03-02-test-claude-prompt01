package com.usermanagement.web.controller;

import com.usermanagement.service.DepartmentService;
import com.usermanagement.service.dto.CreateDepartmentRequest;
import com.usermanagement.service.dto.DepartmentDTO;
import com.usermanagement.service.dto.DepartmentQueryRequest;
import com.usermanagement.service.dto.UpdateDepartmentRequest;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.AssignManagerRequest;
import com.usermanagement.web.dto.MoveDepartmentRequest;
import com.usermanagement.web.dto.PageResponse;
import com.usermanagement.web.dto.UpdateDepartmentStatusRequest;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Department Controller
 * REST API endpoints for department management
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Get all departments with pagination and filters
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<DepartmentDTO>>> getDepartments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer level,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.debug("Getting departments with filters");

        DepartmentQueryRequest query = new DepartmentQueryRequest();
        query.setKeyword(keyword);
        query.setParentId(parentId);
        if (status != null && !status.isEmpty()) {
            try {
                query.setStatus(com.usermanagement.domain.entity.Department.DepartmentStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", status);
            }
        }
        query.setLevel(level);
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);

        Page<DepartmentDTO> departmentPage = departmentService.getDepartments(query);
        PageResponse<DepartmentDTO> pageResponse = PageResponse.from(departmentPage);

        return ResponseEntity.ok(ApiResponse.success("Departments retrieved successfully", pageResponse));
    }

    /**
     * Get department by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartmentById(@PathVariable UUID id) {
        logger.debug("Getting department by ID: {}", id);

        DepartmentDTO department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success("Department retrieved successfully", department));
    }

    /**
     * Get department by code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartmentByCode(@PathVariable String code) {
        logger.debug("Getting department by code: {}", code);

        DepartmentDTO department = departmentService.getDepartmentByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Department retrieved successfully", department));
    }

    /**
     * Create a new department
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DEPT_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        logger.info("Creating new department with code: {}", request.getCode());

        DepartmentDTO createdDepartment = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created successfully", createdDepartment));
    }

    /**
     * Update an existing department
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        logger.info("Updating department with ID: {}", id);

        DepartmentDTO updatedDepartment = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", updatedDepartment));
    }

    /**
     * Update department status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('DEPT_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateDepartmentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDepartmentStatusRequest request) {
        logger.info("Updating department status for ID: {} to {}", id, request.getStatus());

        departmentService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Department status updated successfully", null));
    }

    /**
     * Delete a department (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_DELETE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable UUID id) {
        logger.info("Deleting department with ID: {}", id);

        departmentService.deleteDepartment(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Department deleted successfully", null));
    }

    /**
     * Get department tree structure
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getDepartmentTree(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        logger.debug("Getting department tree, includeInactive: {}", includeInactive);

        List<DepartmentDTO> tree = departmentService.getDepartmentTree(includeInactive);
        return ResponseEntity.ok(ApiResponse.success("Department tree retrieved successfully", tree));
    }

    /**
     * Get children of a department
     */
    @GetMapping("/{id}/children")
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getChildren(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        logger.debug("Getting children of department: {}", id);

        List<DepartmentDTO> children = departmentService.getChildren(id, includeInactive);
        return ResponseEntity.ok(ApiResponse.success("Children retrieved successfully", children));
    }

    /**
     * Move department to new parent
     */
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('DEPT_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> moveDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody MoveDepartmentRequest request) {
        logger.info("Moving department {} to parent {}", id, request.getParentId());

        departmentService.moveDepartment(id, request.getParentId());
        return ResponseEntity.ok(ApiResponse.success("Department moved successfully", null));
    }

    /**
     * Assign manager to department
     */
    @PostMapping("/{id}/manager")
    @PreAuthorize("hasAuthority('DEPT_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignManager(
            @PathVariable UUID id,
            @Valid @RequestBody AssignManagerRequest request) {
        logger.info("Assigning manager {} to department {}", request.getManagerId(), id);

        departmentService.assignManager(id, request.getManagerId());
        return ResponseEntity.ok(ApiResponse.success("Manager assigned successfully", null));
    }

    /**
     * Get all descendant department IDs (including self)
     */
    @GetMapping("/{id}/descendants")
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> getDescendantIds(@PathVariable UUID id) {
        logger.debug("Getting descendant IDs of department: {}", id);

        List<UUID> descendantIds = departmentService.getDescendantIds(id);
        return ResponseEntity.ok(ApiResponse.success("Descendant IDs retrieved successfully", descendantIds));
    }

    /**
     * Check if department code is available
     */
    @GetMapping("/check-code")
    @PreAuthorize("hasAuthority('DEPT_READ') or hasAuthority('DEPT_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeAvailability(
            @RequestParam String code,
            @RequestParam(required = false) UUID excludeId) {
        boolean available = departmentService.isCodeAvailable(code, excludeId);
        String message = available ? "Code is available" : "Code is already taken";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }
}
