package com.usermanagement.service;

import com.usermanagement.domain.entity.Department;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.DepartmentRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.service.dto.CreateDepartmentRequest;
import com.usermanagement.service.dto.DepartmentDTO;
import com.usermanagement.service.dto.DepartmentQueryRequest;
import com.usermanagement.service.dto.UpdateDepartmentRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Department Service Implementation
 * Implements department management business logic with tree structure support
 *
 * @author Service Team
 * @since 1.0
 */
@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentServiceImpl.class);
    private static final int MAX_DEPARTMENT_LEVEL = 5;

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                  UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DepartmentDTO createDepartment(CreateDepartmentRequest request) {
        logger.info("Creating new department with code: {}", request.getCode());

        // Validate code uniqueness
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getCode());
        }

        // Create department entity
        Department department = new Department();
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());
        department.setSortOrder(request.getSortOrder());
        department.setStatus(Department.DepartmentStatus.ACTIVE);

        // Set parent if provided
        if (request.getParentId() != null) {
            Department parent = departmentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent department not found: " + request.getParentId()));

            // Check if parent is deleted
            if (parent.isDeleted()) {
                throw new IllegalArgumentException("Cannot use deleted department as parent: " + request.getParentId());
            }

            // Check max level constraint
            if (parent.getLevel() >= MAX_DEPARTMENT_LEVEL) {
                throw new IllegalArgumentException("Cannot create department beyond level " + MAX_DEPARTMENT_LEVEL);
            }

            department.setParent(parent);
        }

        // Set manager if provided
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + request.getManagerId()));
            department.setManager(manager);
        }

        // Save department (path will be calculated)
        Department savedDepartment = departmentRepository.save(department);

        // Recalculate path with actual ID
        recalculatePath(savedDepartment);
        savedDepartment = departmentRepository.save(savedDepartment);

        logger.info("Department created successfully with ID: {}", savedDepartment.getId());
        return convertToDTO(savedDepartment);
    }

    @Override
    public DepartmentDTO updateDepartment(UUID id, UpdateDepartmentRequest request) {
        logger.info("Updating department with ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted department: " + id);
        }

        // Update code if changed
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(department.getCode())) {
            if (departmentRepository.existsByCode(request.getCode())) {
                throw new IllegalArgumentException("Department code already exists: " + request.getCode());
            }
            department.setCode(request.getCode());
        }

        // Update name if provided
        if (StringUtils.hasText(request.getName())) {
            department.setName(request.getName());
        }

        // Update description
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        // Update sort order
        if (request.getSortOrder() != null) {
            department.setSortOrder(request.getSortOrder());
        }

        // Update manager if provided
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + request.getManagerId()));
            department.setManager(manager);
        } else if (request.getManagerId() == null && department.getManager() != null) {
            // Explicitly remove manager
            department.setManager(null);
        }

        Department updatedDepartment = departmentRepository.save(department);
        logger.info("Department updated successfully: {}", id);
        return convertToDTO(updatedDepartment);
    }

    @Override
    public void deleteDepartment(UUID id) {
        logger.info("Deleting department with ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Department is already deleted: " + id);
        }

        // Check if has children
        long childrenCount = departmentRepository.countByParentId(id);
        if (childrenCount > 0) {
            throw new IllegalArgumentException("Cannot delete department with children. Please delete or move children first.");
        }

        // Check if has users
        long userCount = userRepository.countByDepartmentId(id);
        if (userCount > 0) {
            throw new IllegalArgumentException("Cannot delete department with assigned users. Please reassign users first.");
        }

        // Soft delete
        department.softDelete();
        departmentRepository.save(department);

        logger.info("Department deleted successfully: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Department not found: " + id);
        }

        return convertToDTO(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentByCode(String code) {
        Department department = departmentRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + code));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Department not found: " + code);
        }

        return convertToDTO(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentDTO> getDepartments(DepartmentQueryRequest query) {
        query.normalize();

        Sort sort = Sort.by(
                Sort.Direction.fromString(query.getSortDirection()),
                query.getSortBy()
        );
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        Page<Department> departmentPage;

        // Apply filters
        if (StringUtils.hasText(query.getKeyword())) {
            departmentPage = departmentRepository.findByNameContaining(query.getKeyword(), pageable);
        } else if (query.getStatus() != null) {
            departmentPage = departmentRepository.findByStatus(query.getStatus(), pageable);
        } else if (query.getParentId() != null) {
            List<Department> children = departmentRepository.findByParentId(query.getParentId());
            // Convert to page manually since repository returns List
            departmentPage = new org.springframework.data.domain.PageImpl<>(
                    children,
                    pageable,
                    children.size()
            );
        } else {
            departmentPage = departmentRepository.findAllActive(pageable);
        }

        return departmentPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getDepartmentTree(boolean includeInactive) {
        List<Department> allDepartments;

        if (includeInactive) {
            allDepartments = departmentRepository.findAllActive();
        } else {
            allDepartments = departmentRepository.findByStatus(Department.DepartmentStatus.ACTIVE);
        }

        // Build tree structure
        return buildTree(allDepartments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getChildren(UUID parentId, boolean includeInactive) {
        List<Department> children;

        if (parentId == null) {
            // Get root departments
            children = departmentRepository.findAllActive().stream()
                    .filter(d -> d.getParent() == null)
                    .collect(Collectors.toList());
        } else {
            if (includeInactive) {
                children = departmentRepository.findByParentId(parentId);
            } else {
                children = departmentRepository.findByParentIdAndStatus(parentId, Department.DepartmentStatus.ACTIVE);
            }
        }

        return children.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(UUID id, String statusStr) {
        logger.info("Updating department status for ID: {} to {}", id, statusStr);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Cannot update status of deleted department: " + id);
        }

        Department.DepartmentStatus newStatus;
        try {
            newStatus = Department.DepartmentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }

        department.setStatus(newStatus);
        departmentRepository.save(department);

        logger.info("Department status updated successfully: {} -> {}", id, newStatus);
    }

    @Override
    public void moveDepartment(UUID id, UUID newParentId) {
        logger.info("Moving department {} to new parent {}", id, newParentId);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Cannot move deleted department: " + id);
        }

        // Cannot be its own parent
        if (newParentId != null && newParentId.equals(id)) {
            throw new IllegalArgumentException("Department cannot be its own parent");
        }

        // Cannot move to a descendant (would create circular reference)
        if (newParentId != null) {
            List<UUID> descendantIds = getDescendantIds(id);
            if (descendantIds.contains(newParentId)) {
                throw new IllegalArgumentException("Cannot move department to its descendant");
            }

            Department newParent = departmentRepository.findById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent department not found: " + newParentId));

            if (newParent.isDeleted()) {
                throw new IllegalArgumentException("Cannot move to deleted department: " + newParentId);
            }

            // Check max level constraint
            if (newParent.getLevel() + getDepth(department) > MAX_DEPARTMENT_LEVEL) {
                throw new IllegalArgumentException("Move would exceed maximum department level of " + MAX_DEPARTMENT_LEVEL);
            }

            department.setParent(newParent);
        } else {
            department.setParent(null);
        }

        // Recalculate paths for all descendants
        recalculatePath(department);
        recalculateChildrenPaths(department);

        departmentRepository.save(department);

        logger.info("Department moved successfully: {} -> {}", id, newParentId);
    }

    @Override
    public void assignManager(UUID departmentId, UUID managerId) {
        logger.info("Assigning manager {} to department {}", managerId, departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        if (department.isDeleted()) {
            throw new IllegalArgumentException("Cannot assign manager to deleted department: " + departmentId);
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + managerId));

        if (manager.isDeleted()) {
            throw new IllegalArgumentException("Cannot assign deleted user as manager: " + managerId);
        }

        department.setManager(manager);
        departmentRepository.save(department);

        logger.info("Manager assigned successfully: {} -> {}", managerId, departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getDescendantIds(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        List<UUID> ids = new ArrayList<>();
        ids.add(departmentId);

        // Use BFS to find all descendants
        Queue<UUID> queue = new LinkedList<>();
        queue.add(departmentId);

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            List<Department> children = departmentRepository.findByParentId(currentId);

            for (Department child : children) {
                if (!child.isDeleted()) {
                    ids.add(child.getId());
                    queue.add(child.getId());
                }
            }
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code, UUID excludeId) {
        if (!StringUtils.hasText(code)) {
            return false;
        }

        return departmentRepository.findByCode(code)
                .map(d -> excludeId != null && d.getId().equals(excludeId))
                .orElse(true);
    }

    /**
     * Recalculate path for a department based on its parent
     */
    private void recalculatePath(Department department) {
        if (department.getParent() != null) {
            department.setPath(department.getParent().getPath() + "/" + department.getId());
            department.setLevel(department.getParent().getLevel() + 1);
        } else {
            department.setPath("/" + department.getId());
            department.setLevel(1);
        }
    }

    /**
     * Recalculate paths for all children recursively
     */
    private void recalculateChildrenPaths(Department department) {
        List<Department> children = departmentRepository.findByParentId(department.getId());

        for (Department child : children) {
            if (!child.isDeleted()) {
                recalculatePath(child);
                recalculateChildrenPaths(child);
                departmentRepository.save(child);
            }
        }
    }

    /**
     * Get the maximum depth of a department tree
     */
    private int getDepth(Department department) {
        int maxDepth = 1;

        List<Department> children = departmentRepository.findByParentId(department.getId());
        for (Department child : children) {
            if (!child.isDeleted()) {
                maxDepth = Math.max(maxDepth, 1 + getDepth(child));
            }
        }

        return maxDepth;
    }

    /**
     * Build tree structure from flat list
     */
    private List<DepartmentDTO> buildTree(List<Department> departments) {
        Map<UUID, DepartmentDTO> dtoMap = new HashMap<>();
        List<DepartmentDTO> roots = new ArrayList<>();

        // First pass: create all DTOs
        for (Department dept : departments) {
            dtoMap.put(dept.getId(), convertToDTO(dept));
        }

        // Second pass: build hierarchy
        for (Department dept : departments) {
            DepartmentDTO dto = dtoMap.get(dept.getId());

            if (dept.getParent() == null) {
                roots.add(dto);
            } else {
                DepartmentDTO parentDto = dtoMap.get(dept.getParent().getId());
                if (parentDto != null) {
                    if (parentDto.getChildren() == null) {
                        parentDto.setChildren(new ArrayList<>());
                    }
                    parentDto.getChildren().add(dto);
                }
            }
        }

        // Sort roots by sortOrder
        roots.sort((a, b) -> b.getSortOrder().compareTo(a.getSortOrder()));

        return roots;
    }

    /**
     * Convert Department entity to DepartmentDTO
     */
    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCode(department.getCode());
        dto.setLevel(department.getLevel());
        dto.setPath(department.getPath());
        dto.setSortOrder(department.getSortOrder());
        dto.setDescription(department.getDescription());
        dto.setStatus(department.getStatus());
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());

        // Parent info
        if (department.getParent() != null) {
            dto.setParentId(department.getParent().getId());
            dto.setParentName(department.getParent().getName());
        }

        // Manager info
        if (department.getManager() != null) {
            dto.setManagerId(department.getManager().getId());
            dto.setManagerName(department.getManager().getFullName());
        }

        // User count (subquery in real scenario, simplified here)
        dto.setUserCount(userRepository.countByDepartmentId(department.getId()));

        return dto;
    }
}
