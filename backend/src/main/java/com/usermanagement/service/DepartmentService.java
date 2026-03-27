package com.usermanagement.service;

import com.usermanagement.service.dto.CreateDepartmentRequest;
import com.usermanagement.service.dto.DepartmentDTO;
import com.usermanagement.service.dto.DepartmentQueryRequest;
import com.usermanagement.service.dto.UpdateDepartmentRequest;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Department Service Interface
 * Handles department management operations including CRUD and tree structure
 *
 * @author Service Team
 * @since 1.0
 */
public interface DepartmentService {

    /**
     * Create a new department
     *
     * @param request department creation request
     * @return created department DTO
     */
    DepartmentDTO createDepartment(CreateDepartmentRequest request);

    /**
     * Update an existing department
     *
     * @param id department ID
     * @param request department update request
     * @return updated department DTO
     */
    DepartmentDTO updateDepartment(UUID id, UpdateDepartmentRequest request);

    /**
     * Delete a department (soft delete)
     * Cannot delete if has children or users
     *
     * @param id department ID
     */
    void deleteDepartment(UUID id);

    /**
     * Get department by ID
     *
     * @param id department ID
     * @return department DTO
     */
    DepartmentDTO getDepartmentById(UUID id);

    /**
     * Get department by code
     *
     * @param code department code
     * @return department DTO
     */
    DepartmentDTO getDepartmentByCode(String code);

    /**
     * Get paginated list of departments with optional filters
     *
     * @param query query request with filters and pagination
     * @return page of department DTOs
     */
    Page<DepartmentDTO> getDepartments(DepartmentQueryRequest query);

    /**
     * Get all departments as a tree structure
     *
     * @param includeInactive whether to include inactive departments
     * @return list of root department DTOs with children
     */
    List<DepartmentDTO> getDepartmentTree(boolean includeInactive);

    /**
     * Get children of a department
     *
     * @param parentId parent department ID (null for root)
     * @param includeInactive whether to include inactive departments
     * @return list of child department DTOs
     */
    List<DepartmentDTO> getChildren(UUID parentId, boolean includeInactive);

    /**
     * Update department status
     *
     * @param id department ID
     * @param status new status
     */
    void updateStatus(UUID id, String status);

    /**
     * Move department to new parent
     *
     * @param id department ID
     * @param newParentId new parent department ID (null for root)
     */
    void moveDepartment(UUID id, UUID newParentId);

    /**
     * Assign manager to department
     *
     * @param departmentId department ID
     * @param managerId user ID to be manager
     */
    void assignManager(UUID departmentId, UUID managerId);

    /**
     * Get all descendant department IDs (including self)
     *
     * @param departmentId department ID
     * @return list of department IDs
     */
    List<UUID> getDescendantIds(UUID departmentId);

    /**
     * Check if code is available (not used by other department)
     *
     * @param code department code
     * @param excludeId exclude this department ID (for updates)
     * @return true if available
     */
    boolean isCodeAvailable(String code, UUID excludeId);
}
