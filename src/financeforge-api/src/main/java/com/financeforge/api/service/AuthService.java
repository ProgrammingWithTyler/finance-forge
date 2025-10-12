package com.financeforge.api.service;

import com.financeforge.api.domain.model.User;
import com.financeforge.api.domain.model.enums.UserStatus;
import com.financeforge.api.dto.auth.*;
import com.financeforge.api.repository.UserRepository;
import com.financeforge.api.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user authentication and authorization operations.
 * <p>
 * This service handles all authentication-related business logic including:
 * <ul>
 *   <li>User registration with password hashing</li>
 *   <li>User login with credential validation</li>
 *   <li>JWT token generation (access + refresh)</li>
 *   <li>Token refresh for session extension</li>
 *   <li>Current user retrieval</li>
 * </ul>
 * <p>
 * <b>Security Architecture:</b>
 * <pre>
 * Registration Flow:
 * 1. Validate password complexity (uppercase, lowercase, digit, special char)
 * 2. Check username uniqueness (via UserRepository)
 * 3. Check email uniqueness (via UserRepository)
 * 4. Hash password with BCrypt (12 rounds)
 * 5. Store user with hashed password (NEVER plaintext)
 * 6. Generate JWT tokens
 * 7. Return AuthResponse
 *
 * Login Flow:
 * 1. Find user by username or email
 * 2. Check user status (must be ACTIVE)
 * 3. Authenticate with Spring Security (verifies password hash)
 * 4. Generate JWT tokens
 * 5. Return AuthResponse
 *
 * Token Refresh Flow:
 * 1. Validate token is a refresh token (not access token)
 * 2. Extract username from token
 * 3. Validate token signature and expiration
 * 4. Generate new access + refresh tokens
 * 5. Return AuthResponse
 * </pre>
 * <p>
 * <b>Password Security:</b>
 * <ul>
 *   <li>Passwords NEVER sent to database in plaintext</li>
 *   <li>BCrypt hashing with 12 rounds (industry standard)</li>
 *   <li>Password complexity enforced: uppercase, lowercase, digit, special char</li>
 *   <li>Minimum 8 characters required (enforced by DTO validation)</li>
 * </ul>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see JwtService
 * @see UserRepository
 * @see PasswordEncoder
 */
@Service
public class AuthService {

    private final static Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Constructs a new AuthService with required dependencies.
     *
     * @param userRepository repository for user data access
     * @param passwordEncoder BCrypt encoder for password hashing
     * @param jwtService service for JWT token operations
     * @param authenticationManager Spring Security authentication manager
     */
    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user account.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Validates password complexity requirements</li>
     *   <li>Checks that username is not already taken</li>
     *   <li>Checks that email is not already registered</li>
     *   <li>Hashes the password using BCrypt</li>
     *   <li>Creates user in database via USER_MGMT_PKG</li>
     *   <li>Generates JWT access and refresh tokens</li>
     *   <li>Returns authentication response with tokens</li>
     * </ol>
     * <p>
     * <b>Security Notes:</b>
     * <ul>
     *   <li>Password is hashed BEFORE being sent to database</li>
     *   <li>New users start with PENDING status by default</li>
     *   <li>Audit log is created by USER_MGMT_PKG.CREATE_USER</li>
     * </ul>
     *
     * @param request registration request containing user details and password
     * @return authentication response with JWT tokens and user info
     * @throws IllegalArgumentException if password doesn't meet complexity requirements
     * @throws IllegalStateException if username or email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for username: {}", request.username());

        // 1. Validate password complexity
        if (!request.hasValidPasswordComplexity()) {
            log.warn("Registration failed for {}: weak password", request.username());
            throw new IllegalArgumentException(
                "Password must contain at least one uppercase letter, one lowercase letter, " +
                    "one digit, and one special character"
            );
        }

        // 2. Check username doesn't exist
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Registration failed: username '{}' already taken", request.username());
            throw new IllegalStateException("Username is already taken");
        }

        // 3. Check email doesn't exist
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration failed: email '{}' already registered", request.email());
            throw new IllegalStateException("Email is already registered");
        }

        // 4. Hash password with BCrypt (CRITICAL: Before sending to DB)
        String hashedPassword = passwordEncoder.encode(request.password());
        log.debug("Password hashed successfully for user: {}", request.username());

        // 5. Create user entity
        User user = User.builder()
            .username(request.username())
            .email(request.email())
            .passwordHash(hashedPassword)
            .firstName(request.firstName())
            .lastName(request.lastName())
            .status(UserStatus.ACTIVE) // TODO: Will change PENDING status back when email verification is done
            .createdBy("REGISTRATION")
            .build();

        // 6. Save to database via USER_MGMT_PKG
        Long userId = userRepository.createUser(user);
        user.setId(userId);
        log.info("User registered successfully: id={}, username={}", userId, request.username());

        // 7. Generate JWT tokens
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .authorities("ROLE_USER")
            .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Long expiresIn = jwtService.getAccessTokenExpirationInSeconds();

        log.debug("JWT tokens generated for user: {}", user.getUsername());

        // 8. Build response
        UserInfo userInfo = new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName()
        );

        return new AuthResponse(accessToken, refreshToken, expiresIn, userInfo);
    }

    /**
     * Authenticates a user and generates JWT tokens.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Finds user by username or email</li>
     *   <li>Checks user account status (must be ACTIVE)</li>
     *   <li>Authenticates credentials with Spring Security</li>
     *   <li>Generates JWT access and refresh tokens</li>
     *   <li>Returns authentication response with tokens</li>
     * </ol>
     * <p>
     * <b>Security Notes:</b>
     * <ul>
     *   <li>Spring Security verifies password hash (BCrypt.matches)</li>
     *   <li>INACTIVE users cannot log in</li>
     *   <li>PENDING users cannot log in (email verification required)</li>
     *   <li>Failed login attempts are logged for security monitoring</li>
     * </ul>
     *
     * @param request login request containing username/email and password
     * @return authentication response with JWT tokens and user info
     * @throws UsernameNotFoundException if user doesn't exist
     * @throws IllegalStateException if user account is not ACTIVE
     * @throws BadCredentialsException if password is incorrect
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.usernameOrEmail());

        // 1. Find user by username or email
        User user = userRepository.findByUsername(request.usernameOrEmail())
            .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
            .orElseThrow(() -> {
            log.warn("Login failed: user not found '{}'", request.usernameOrEmail());
            return new UsernameNotFoundException("Invalid username or password");
        });

        // 2. Check user status
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login failed: user '{}' has status {}", user.getUsername(), user.getStatus());
            throw new IllegalStateException(
                "Account is not active. Please contact support or verify your email."
            );
        }

        // 3. Authenicate with Spring Security (verifies password hash)
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    request.password()
                )
            );
            log.info("Login successful for user: {}", user.getUsername());
        } catch (BadCredentialsException e) {
            log.warn("Login failed: invalid credentials for '{}'", request.usernameOrEmail());
            throw new BadCredentialsException("Invalid username or password");
        }

        // 4. Generate JWT tokens
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .authorities("ROLE_USER")
            .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Long expiresIn = jwtService.getAccessTokenExpirationInSeconds();

        log.debug("JWT tokens generated for user: {}", user.getUsername());

        // 8. build response
        UserInfo userInfo = new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName()
        );

        return new AuthResponse(accessToken, refreshToken, expiresIn, userInfo);
    }

    /**
     * Refreshes JWT tokens using a valid refresh token.
     * <p>
     * This method allows users to obtain new access tokens without re-authenticating.
     * It performs the following steps:
     * <ol>
     *   <li>Validates that the token is a refresh token (not access token)</li>
     *   <li>Extracts username from the refresh token</li>
     *   <li>Validates token signature and expiration</li>
     *   <li>Loads user from database</li>
     *   <li>Generates new access and refresh tokens</li>
     *   <li>Returns authentication response with new tokens</li>
     * </ol>
     * <p>
     * <b>Security Notes:</b>
     * <ul>
     *   <li>Refresh tokens cannot be used as access tokens</li>
     *   <li>Expired refresh tokens are rejected</li>
     *   <li>Both new access AND refresh tokens are returned</li>
     *   <li>User must still exist and be ACTIVE</li>
     * </ul>
     *
     * @param request refresh token request containing the refresh token
     * @return authentication response with new JWT tokens and user info
     * @throws IllegalArgumentException if token is not a valid refresh token
     * @throws UsernameNotFoundException if user no longer exists
     * @throws IllegalStateException if user account is not ACTIVE
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Token refresh attempt");

        // 1. Validate it's a refresh token (not an access token)
        if (!jwtService.isRefreshToken(request.refreshToken())) {
            log.warn("Token refresh failed: not a refresh token");
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 2. Extract username from token
        String username = jwtService.extractUsername(request.refreshToken());
        log.debug("Token refresh for user: {}", username);

        // 3. Load user from database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("Token refresh failed: user '{}' not found", username);
                return new UsernameNotFoundException("User not found");
            });
        // 4. Check user status
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Token refresh failed: user '{}' not active", username);
            throw new IllegalStateException("Account is not active");
        }

        // 5. Create UserDetails for token generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .authorities("ROLE_USER")
            .build();

        // 6. Validate token and generate new tokens
        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            log.warn("Token refresh failed: invalid or expired token for '{}'", username);
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Long expiresIn = jwtService.getAccessTokenExpirationInSeconds();

        log.info("Token refreshed successfully for user: {}", username);

        // 7. build response
        UserInfo userInfo = new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName()
        );

        return new AuthResponse(accessToken, refreshToken, expiresIn, userInfo);
    }

    /**
     * Retrieves the current authenticated user's details.
     * <p>
     * This method is used by the GET /auth/me endpoint to return
     * comprehensive user information for the authenticated user.
     *
     * @param username the username of the authenticated user (from JWT)
     * @return current user response with full user details
     * @throws UsernameNotFoundException if user doesn't exist
     */
    public CurrentUserResponse getCurrentUser(String username) {
        log.debug("Fetching current user details for: {}", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("Current user lookup failed: '{}' not found", username);
                return new UsernameNotFoundException("User not found");
            });

        return new CurrentUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
