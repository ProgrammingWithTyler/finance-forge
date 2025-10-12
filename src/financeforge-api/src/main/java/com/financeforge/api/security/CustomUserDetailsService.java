package com.financeforge.api.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.financeforge.api.domain.model.User;
import com.financeforge.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 * <p>
 * This service acts as a bridge between our domain User entity and Spring Security's UserDetails interface.
 * It loads user information from the database and converts it into a format that Spring Security can use
 * for authentication and authorization.
 * <p>
 * <b>How It Works:</b>
 * <pre>
 * 1. Spring Security needs to authenticate a user
 * 2. It calls loadUserByUsername(username)
 * 3. We query UserRepository for the User entity
 * 4. We convert User → Spring Security UserDetails
 * 5. Spring Security uses UserDetails to verify credentials
 * </pre>
 * <p>
 * <b>When This Is Called:</b>
 * <ul>
 *   <li>During login (AuthenticationManager.authenticate)</li>
 *   <li>When validating JWT tokens (JwtAuthenticationFilter)</li>
 *   <li>Any time Spring Security needs user details</li>
 * </ul>
 * <p>
 * <b>UserDetails vs User Entity:</b>
 * <pre>
 * User (our entity):              UserDetails (Spring Security):
 * - id                            - username
 * - username                      - password (hash)
 * - email                         - authorities (roles/permissions)
 * - passwordHash                  - enabled
 * - firstName                     - accountNonExpired
 * - lastName                      - accountNonLocked
 * - status                        - credentialsNonExpired
 * - createdAt
 * - updatedAt
 * </pre>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see UserDetailsService
 * @see UserDetails
 * @see User
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    /**
     * Constructs a new CustomUserDetailsService.
     *
     * @param userRepository repository for loading user data from database
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by username for Spring Security authentication.
     * <p>
     * This method is called by Spring Security during authentication to retrieve
     * user information. It performs the following steps:
     * <ol>
     *   <li>Queries the database for a user with the given username</li>
     *   <li>Throws UsernameNotFoundException if user doesn't exist</li>
     *   <li>Converts our User entity to Spring Security's UserDetails</li>
     *   <li>Sets authorities/roles (currently just "ROLE_USER")</li>
     *   <li>Returns UserDetails for Spring Security to verify password</li>
     * </ol>
     * <p>
     * <b>Security Notes:</b>
     * <ul>
     *   <li>This method does NOT verify the password - Spring Security does that</li>
     *   <li>We return the password hash so Spring Security can compare with BCrypt</li>
     *   <li>All users get "ROLE_USER" authority by default</li>
     *   <li>Future enhancement: support multiple roles from database</li>
     * </ul>
     * <p>
     * <b>Account Status Handling:</b>
     * <ul>
     *   <li>ACTIVE users: enabled = true (can log in)</li>
     *   <li>INACTIVE users: enabled = false (cannot log in)</li>
     *   <li>PENDING users: enabled = false (cannot log in until verified)</li>
     * </ul>
     *
     * @param username the username to search for (case-sensitive)
     * @return UserDetails object containing user info and authorities
     * @throws UsernameNotFoundException if no user found with the given username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        // 1. Find user in database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("User not found: {}", username);
                return new UsernameNotFoundException("User not found: " + username);
            });

        log.debug("User found: id={}, status={}", user.getId(), user.getStatus());

        // 2. Map our User entity to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPasswordHash())  // Spring Security needs the hash to verify
            .authorities(getAuthorities(user)) // Convert roles to GrantedAuthorities
            .accountExpired(false)              // We don't track account expiration
            .accountLocked(false)               // We don't track account locking
            .credentialsExpired(false)          // We don't track credential expiration
            .disabled(!user.isActive())         // Only ACTIVE users can log in
            .build();
    }

    /**
     * Converts user status/roles into Spring Security authorities.
     * <p>
     * Spring Security uses the {@link org.springframework.security.core.GrantedAuthority}
     * interface to represent roles and permissions. This method creates a list of
     * authorities based on the user's status and role.
     * <p>
     * <b>Current Implementation:</b>
     * All users get "ROLE_USER" by default. This is a simple role system suitable
     * for the MVP phase.
     * <p>
     * <b>Future Enhancements:</b>
     * <pre>
     * - Load roles from USERS.role column (ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR)
     * - Support multiple roles per user (USER_ROLES junction table)
     * - Support fine-grained permissions (USER_PERMISSIONS table)
     * - Example:
     *   if (user.getRole() == UserRole.ADMIN) {
     *       authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
     *       authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_USERS"));
     *   }
     * </pre>
     * <p>
     * <b>Spring Security Role Conventions:</b>
     * <ul>
     *   <li>Roles should be prefixed with "ROLE_" (e.g., "ROLE_USER")</li>
     *   <li>Permissions don't need a prefix (e.g., "READ_EXPENSES")</li>
     *   <li>Both are represented as GrantedAuthority objects</li>
     * </ul>
     *
     * @param user the user entity to extract authorities from
     * @return list of granted authorities for this user
     */
    private List<SimpleGrantedAuthority> getAuthorities(User user) {
        // For MVP: All users get ROLE_USER
        // Future: Load roles from database (ROLE_ADMIN, ROLE_MODERATOR, etc.)
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
