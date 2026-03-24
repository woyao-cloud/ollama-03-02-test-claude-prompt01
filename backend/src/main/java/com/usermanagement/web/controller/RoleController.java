package com.usermanagement.web.controller;

import com.usermanagement.domain.entity.Role;
import com.usermanagement.service.RoleService;
import com.usermanagement.service.dto.CreateRoleRequest;
import com.usermanagement.service.dto.PermissionDTO;
import com.usermanagement.service.dto.RoleDTO;
import com.usermanagement.service.dto.RoleQueryRequest;
import com.usermanagement.service.dto.UpdateRoleRequest;
import com.usermanagement.web.dto.ApiResponse;
import com.usermanagement.web.dto.AssignPermissionsRequest;
import com.usermanagement.web.dto.PageResponse;
import com.usermanagement.web.dto.UpdateDataScopeRequest;

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
 * Role Controller
 * REST API endpoints for role management
 *
 * @author Web Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Get all roles with pagination and filters
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<RoleDTO>>> getRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role.RoleStatus status,
            @RequestParam(required = false) Role.DataScope dataScope,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.debug("Getting roles with filters");

        RoleQueryRequest query = new RoleQueryRequest();
        query.setKeyword(keyword);
        query.setStatus(status);
        query.setDataScope(dataScope);
        query.setPage(page);
        query.setSize(size);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);

        Page<RoleDTO> rolePage = roleService.getRoles(query);
        PageResponse<RoleDTO> pageResponse = PageResponse.from(rolePage);

        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", pageResponse));
    }

    /**
     * Get all active roles (for dropdown selection)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllActiveRoles() {
        logger.debug("Getting all active roles");

        List<RoleDTO> roles = roleService.getAllActiveRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }

    /**
     * Get role by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleById(@PathVariable UUID id) {
        logger.debug("Getting role by ID: {}", id);

        RoleDTO role = roleService.getRoleByIdWithPermissions(id);
        return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", role));
    }

    /**
     * Get role by code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleByCode(@PathVariable String code) {
        logger.debug("Getting role by code: {}", code);

        RoleDTO role = roleService.getRoleByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", role));
    }

    /**
     * Create a new role
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        logger.info("Creating new role with code: {}", request.getCode());

        RoleDTO createdRole = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", createdRole));
    }

    /**
     * Update an existing role
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        logger.info("Updating role with ID: {}", id);

        RoleDTO updatedRole = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", updatedRole));
    }

    /**
     * Delete a role
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        logger.info("Deleting role with ID: {}", id);

        roleService.deleteRole(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Role deleted successfully", null));
    }

    /**
     * Assign permissions to a role
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignPermissions(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        logger.info("Assigning permissions to role: {}, permissions: {}", id, request.getPermissionIds());

        roleService.assignPermissions(id, request.getPermissionIds());
        return ResponseEntity.ok(ApiResponse.success("Permissions assigned successfully", null));
    }

    /**
     * Get role permissions
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getRolePermissions(@PathVariable UUID id) {
        logger.debug("Getting permissions for role: {}", id);

        List<PermissionDTO> permissions = roleService.getRolePermissions(id);
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
    }

    /**
     * Get role permission codes
     */
    @GetMapping("/{id}/permission-codes")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getRolePermissionCodes(@PathVariable UUID id) {
        logger.debug("Getting permission codes for role: {}", id);

        List<String> permissionCodes = roleService.getRolePermissionCodes(id);
        return ResponseEntity.ok(ApiResponse.success("Permission codes retrieved successfully", permissionCodes));
    }

    /**
     * Update role data scope
     */
    @PatchMapping("/{id}/data-scope")
    @PreAuthorize("hasAuthority('ROLE_UPDATE') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RoleDTO>> updateDataScope(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDataScopeRequest request) {
        logger.info("Updating data scope for role: {} to {}", id, request.getDataScope());

        RoleDTO updatedRole = roleService.updateDataScope(id, request.getDataScope());
        return ResponseEntity.ok(ApiResponse.success("Data scope updated successfully", updatedRole));
    }

    /**
     * Check if role code exists
     */
    @GetMapping("/check-code")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeExists(@RequestParam String code) {
        boolean exists = roleService.existsByCode(code);
        String message = exists ? "Role code already exists" : "Role code is available";
        return ResponseEntity.ok(ApiResponse.success(message, exists));
    }

    /**
     * Get roles by user ID
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getRolesByUserId(@PathVariable UUID userId) {
        logger.debug("Getting roles for user: {}", userId);

        List<RoleDTO> roles = roleService.getRolesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("User roles retrieved successfully", roles));
    }
}
