package com.usermanagement.security;

import com.usermanagement.domain.entity.Permission;
import com.usermanagement.domain.entity.Role;
import com.usermanagement.domain.entity.User;
import com.usermanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User Details Service Implementation
 * Loads user details for Spring Security authentication
 *
 * @author Security Team
 * @since 1.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);

        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Build authorities from roles and permissions
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add role authorities
        user.getUserRoles().forEach(userRole -> {
            Role role = userRole.getRole();
            if (role != null && role.isActive()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()));

                // Add permissions from role
                role.getRolePermissions().forEach(rolePermission -> {
                    Permission permission = rolePermission.getPermission();
                    if (permission != null) {
                        authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                    }
                });
            }
        });

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                true, // account non-expired
                true, // credentials non-expired
                !user.isLocked(), // account non-locked
                authorities
        );
    }

    /**
     * Load user by ID
     */
    @Transactional(readOnly = true)
    public User loadUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
}
