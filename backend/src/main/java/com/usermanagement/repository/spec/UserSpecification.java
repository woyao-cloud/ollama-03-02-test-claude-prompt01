package com.usermanagement.repository.spec;

import com.usermanagement.domain.entity.User;
import com.usermanagement.service.dto.UserQueryRequest;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * User Specification
 * Builds dynamic queries for User entity using JPA Criteria API
 *
 * @author Repository Team
 * @since 1.0
 */
public class UserSpecification {

    /**
     * Build specification from query request
     *
     * @param query user query request
     * @return specification for filtering users
     */
    public static Specification<User> withQuery(UserQueryRequest query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter out soft deleted users
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Email filter (partial match, case insensitive)
            if (query.getEmail() != null && !query.getEmail().isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + query.getEmail().toLowerCase() + "%"
                ));
            }

            // First name filter (partial match, case insensitive)
            if (query.getFirstName() != null && !query.getFirstName().isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("firstName")),
                        "%" + query.getFirstName().toLowerCase() + "%"
                ));
            }

            // Last name filter (partial match, case insensitive)
            if (query.getLastName() != null && !query.getLastName().isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("lastName")),
                        "%" + query.getLastName().toLowerCase() + "%"
                ));
            }

            // Phone filter (partial match)
            if (query.getPhone() != null && !query.getPhone().isEmpty()) {
                predicates.add(cb.like(
                        root.get("phone"),
                        "%" + query.getPhone() + "%"
                ));
            }

            // Status filter
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }

            // Department filter
            if (query.getDepartmentId() != null) {
                predicates.add(cb.equal(
                        root.get("department").get("id"),
                        query.getDepartmentId()
                ));
            }

            // Role filter (requires join with user_roles and roles)
            if (query.getRoleId() != null) {
                Join<Object, Object> userRoles = root.join("userRoles");
                Join<Object, Object> role = userRoles.join("role");
                predicates.add(cb.equal(role.get("id"), query.getRoleId()));

                // Ensure distinct results when joining
                criteriaQuery.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification for active users only
     */
    public static Specification<User> isActive() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), User.UserStatus.ACTIVE);
    }

    /**
     * Specification for not deleted users
     */
    public static Specification<User> notDeleted() {
        return (root, query, cb) ->
                cb.isNull(root.get("deletedAt"));
    }

    /**
     * Specification for users by email (exact match)
     */
    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("email")), email.toLowerCase());
    }

    /**
     * Specification for users by department
     */
    public static Specification<User> hasDepartment(String departmentId) {
        return (root, query, cb) ->
                cb.equal(root.get("department").get("id"), departmentId);
    }

    /**
     * Specification for users by status
     */
    public static Specification<User> hasStatus(User.UserStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    /**
     * Specification for locked users
     */
    public static Specification<User> isLocked() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), User.UserStatus.LOCKED);
    }

    /**
     * Specification for users with expired password (placeholder for future implementation)
     */
    public static Specification<User> hasExpiredPassword(int days) {
        return (root, query, cb) -> {
            // Implementation depends on password policy
            // For now, return always true
            return cb.conjunction();
        };
    }

    /**
     * Specification combining email search across first name, last name, and email
     */
    public static Specification<User> fullTextSearch(String searchTerm) {
        return (root, criteriaQuery, cb) -> {
            if (searchTerm == null || searchTerm.isEmpty()) {
                return cb.conjunction();
            }

            String term = "%" + searchTerm.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("email")), term));
            predicates.add(cb.like(cb.lower(root.get("firstName")), term));
            predicates.add(cb.like(cb.lower(root.get("lastName")), term));

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
