package com.usermanagement.web.controller;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.service.PermissionService;
import com.usermanagement.service.dto.CreatePermissionRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.PermissionTreeDTO;
import com.usermanagement.service.dto.UpdatePermissionRequest;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.PageResponse;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Permission Controller
 * REST API endpoints for permission management
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Get all permissions with pagination
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PermissionDTO>>> getPermissions(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        logger.debug("Getting permissions with pagination");

        Sort sort = Sort.by("DESC".equalsIgnoreCase(sortDirection) ?
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PermissionDTO> permissionPage = permissionService.getPermissions(pageable);
        PageResponse<PermissionDTO> pageResponse = PageResponse.from(permissionPage);

        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", pageResponse));
    }

    /**
     * Get all active permissions
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getAllActivePermissions() {
        logger.debug("Getting all active permissions");

        List<PermissionDTO> permissions = permissionService.getAllActivePermissions();
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
    }

    /**
     * Get permission tree structure
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionTreeDTO>>> getPermissionTree() {
        logger.debug("Getting permission tree");

        List<PermissionTreeDTO> tree = permissionService.getPermissionTree();
        return ResponseEntity.ok(ApiResponse.success("Permission tree retrieved successfully", tree));
    }

    /**
     * Get menu permissions
     */
    @GetMapping("/menu")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getMenuPermissions() {
        logger.debug("Getting menu permissions");

        List<PermissionDTO> permissions = permissionService.getMenuPermissions();
        return ResponseEntity.ok(ApiResponse.success("Menu permissions retrieved successfully", permissions));
    }

    /**
     * Get permissions by type
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getPermissionsByType(
            @PathVariable Permission.PermissionType type) {
        logger.debug("Getting permissions by type: {}", type);

        List<PermissionDTO> permissions = permissionService.getPermissionsByType(type);
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
    }

    /**
     * Get permission by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionDTO>> getPermissionById(@PathVariable UUID id) {
        logger.debug("Getting permission by ID: {}", id);

        PermissionDTO permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(ApiResponse.success("Permission retrieved successfully", permission));
    }

    /**
     * Get permission by code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionDTO>> getPermissionByCode(@PathVariable String code) {
        logger.debug("Getting permission by code: {}", code);

        PermissionDTO permission = permissionService.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + code));
        return ResponseEntity.ok(ApiResponse.success("Permission retrieved successfully", permission));
    }

    /**
     * Create a new permission
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionDTO>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        logger.info("Creating new permission with code: {}", request.getCode());

        PermissionDTO createdPermission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Permission created successfully", createdPermission));
    }

    /**
     * Update an existing permission
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionDTO>> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        logger.info("Updating permission with ID: {}", id);

        PermissionDTO updatedPermission = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(ApiResponse.success("Permission updated successfully", updatedPermission));
    }

    /**
     * Delete a permission
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable UUID id) {
        logger.info("Deleting permission with ID: {}", id);

        permissionService.deletePermission(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Permission deleted successfully", null));
    }

    /**
     * Get permissions by resource
     */
    @GetMapping("/resource/{resource}")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getPermissionsByResource(
            @PathVariable String resource) {
        logger.debug("Getting permissions by resource: {}", resource);

        List<PermissionDTO> permissions = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
    }

    /**
     * Check if permission code exists
     */
    @GetMapping("/check-code")
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeExists(@RequestParam String code) {
        boolean exists = permissionService.existsByCode(code);
        String message = exists ? "Permission code already exists" : "Permission code is available";
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }

    /**
     * Initialize default permissions
     */
    @PostMapping("/init-defaults")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> initializeDefaultPermissions() {
        logger.info("Initializing default permissions");

        permissionService.initializeDefaultPermissions();
        return ResponseEntity.ok(ApiResponse.success("Default permissions initialized successfully", null));
    }
}
