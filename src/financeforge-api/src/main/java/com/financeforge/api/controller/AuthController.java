package com.financeforge.api.controller;

import com.financeforge.api.dto.auth.*;
import com.financeforge.api.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and user management endpoints.
 * <p>
 * This controller exposes the authentication API for the FinanceForge application:
 * <ul>
 *   <li>User registration</li>
 *   <li>User login (JWT token generation)</li>
 *   <li>Token refresh (extend session without re-login)</li>
 *   <li>Current user retrieval (get authenticated user info)</li>
 *   <li>Health check (service availability)</li>
 * </ul>
 * <p>
 * <b>API Design Principles:</b>
 * <ul>
 *   <li>RESTful endpoints with proper HTTP methods (POST for mutations, GET for queries)</li>
 *   <li>Meaningful HTTP status codes (201 for creation, 200 for success, 401 for auth failures)</li>
 *   <li>Request validation with @Valid annotations</li>
 *   <li>Consistent response format (AuthResponse for auth operations)</li>
 *   <li>Security-first approach (protected vs public endpoints)</li>
 * </ul>
 * <p>
 * <b>Endpoint Security:</b>
 * <pre>
 * Public Endpoints (No Authentication Required):
 * - POST /api/v1/auth/register   → Anyone can create account
 * - POST /api/v1/auth/login      → Anyone can log in
 * - POST /api/v1/auth/refresh    → Anyone can refresh token
 * - GET  /api/v1/auth/health     → Anyone can check service status
 *
 * Protected Endpoints (JWT Required):
 * - GET  /api/v1/auth/me         → Must be authenticated
 * </pre>
 * <p>
 * <b>Authentication Flow:</b>
 * <pre>
 * 1. Registration:
 *    POST /register { username, email, password, ... }
 *    → Returns JWT tokens immediately (user can log in right away)
 *
 * 2. Login:
 *    POST /login { usernameOrEmail, password }
 *    → Returns JWT tokens if credentials valid
 *
 * 3. Protected Request:
 *    GET /me
 *    Headers: Authorization: Bearer <accessToken>
 *    → Returns current user info
 *
 * 4. Token Refresh:
 *    POST /refresh { refreshToken }
 *    → Returns new access + refresh tokens
 * </pre>
 * <p>
 * <b>Error Handling:</b>
 * All exceptions are handled by {@link com.financeforge.api.exception.GlobalExceptionHandler}
 * which returns standardized error responses with appropriate HTTP status codes.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see AuthService
 * @see AuthResponse
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Constructs a new AuthController.
     *
     * @param authService service for authentication operations
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     * <p>
     * This endpoint creates a new user with the provided credentials and information.
     * Upon successful registration, JWT tokens are immediately generated and returned,
     * allowing the user to access protected endpoints without a separate login request.
     * <p>
     * <b>Request Validation:</b>
     * <ul>
     *   <li>Username: 3-50 characters, must be unique</li>
     *   <li>Email: Valid email format, must be unique</li>
     *   <li>Password: 8-100 characters, must meet complexity requirements</li>
     *   <li>First Name: Required, max 100 characters</li>
     *   <li>Last Name: Required, max 100 characters</li>
     * </ul>
     * <p>
     * <b>Password Complexity Requirements:</b>
     * <ul>
     *   <li>At least one uppercase letter (A-Z)</li>
     *   <li>At least one lowercase letter (a-z)</li>
     *   <li>At least one digit (0-9)</li>
     *   <li>At least one special character (!@#$%^&*...)</li>
     * </ul>
     * <p>
     * <b>Success Response (201 CREATED):</b>
     * <pre>
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 900,
     *   "user": {
     *     "id": 1,
     *     "username": "johndoe",
     *     "email": "john@example.com",
     *     "firstName": "John",
     *     "lastName": "Doe"
     *   }
     * }
     * </pre>
     * <p>
     * <b>Error Responses:</b>
     * <ul>
     *   <li>400 Bad Request: Validation errors (weak password, invalid email, etc.)</li>
     *   <li>409 Conflict: Username or email already exists</li>
     *   <li>500 Internal Server Error: Unexpected server error</li>
     * </ul>
     *
     * @param request registration request containing user details
     * @return response entity with JWT tokens and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.username());

        AuthResponse response = authService.register(request);

        log.info("User registered successfully: {}", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and generates JWT tokens.
     * <p>
     * This endpoint validates user credentials and returns JWT tokens if authentication
     * is successful. The access token is used for API requests, while the refresh token
     * is used to obtain new access tokens without re-entering credentials.
     * <p>
     * <b>Authentication Process:</b>
     * <ol>
     *   <li>User submits username/email and password</li>
     *   <li>System looks up user in database</li>
     *   <li>System verifies password using BCrypt</li>
     *   <li>System checks account status (must be ACTIVE)</li>
     *   <li>System generates JWT access and refresh tokens</li>
     *   <li>Tokens are returned to client</li>
     * </ol>
     * <p>
     * <b>Success Response (200 OK):</b>
     * <pre>
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 900,
     *   "user": {
     *     "id": 1,
     *     "username": "johndoe",
     *     "email": "john@example.com",
     *     "firstName": "John",
     *     "lastName": "Doe"
     *   }
     * }
     * </pre>
     * <p>
     * <b>Error Responses:</b>
     * <ul>
     *   <li>400 Bad Request: Missing or invalid credentials</li>
     *   <li>401 Unauthorized: Invalid username/password or account not active</li>
     *   <li>500 Internal Server Error: Unexpected server error</li>
     * </ul>
     * <p>
     * <b>Security Notes:</b>
     * <ul>
     *   <li>Failed login attempts are logged for security monitoring</li>
     *   <li>Generic error messages prevent username enumeration</li>
     *   <li>INACTIVE and PENDING accounts cannot log in</li>
     * </ul>
     *
     * @param request login request containing username/email and password
     * @return response entity with JWT tokens and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for: {}", request.usernameOrEmail());

        AuthResponse response = authService.login(request);

        log.info("User logged in successfully: {}", request.usernameOrEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes JWT tokens using a valid refresh token.
     * <p>
     * This endpoint allows clients to obtain new access tokens without requiring
     * the user to re-authenticate. This is useful for maintaining long-lived sessions
     * while keeping access tokens short-lived for security.
     * <p>
     * <b>Token Refresh Flow:</b>
     * <ol>
     *   <li>Client sends refresh token</li>
     *   <li>System validates refresh token (signature + expiration)</li>
     *   <li>System verifies user still exists and is ACTIVE</li>
     *   <li>System generates new access AND refresh tokens</li>
     *   <li>New tokens are returned to client</li>
     * </ol>
     * <p>
     * <b>Why Refresh Both Tokens?</b>
     * Rotating refresh tokens provides better security:
     * <ul>
     *   <li>If refresh token is compromised, it has limited lifetime</li>
     *   <li>Old refresh tokens become invalid after use</li>
     *   <li>Reduces risk of token replay attacks</li>
     * </ul>
     * <p>
     * <b>Success Response (200 OK):</b>
     * <pre>
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 900,
     *   "user": { ... }
     * }
     * </pre>
     * <p>
     * <b>Error Responses:</b>
     * <ul>
     *   <li>400 Bad Request: Missing or invalid refresh token</li>
     *   <li>401 Unauthorized: Expired or invalid refresh token</li>
     *   <li>500 Internal Server Error: Unexpected server error</li>
     * </ul>
     *
     * @param request refresh token request containing the refresh token
     * @return response entity with new JWT tokens and user info
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");

        AuthResponse response = authService.refreshToken(request);

        log.debug("Tokens refreshed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current authenticated user's information.
     * <p>
     * This endpoint returns detailed information about the currently authenticated user.
     * It requires a valid JWT token in the Authorization header.
     * <p>
     * <b>Authentication Required:</b>
     * This endpoint is protected by Spring Security. Requests must include:
     * <pre>
     * Authorization: Bearer <accessToken>
     * </pre>
     * <p>
     * The {@link AuthenticationPrincipal} annotation automatically extracts the
     * authenticated user from the SecurityContext (set by JwtAuthenticationFilter).
     * <p>
     * <b>Success Response (200 OK):</b>
     * <pre>
     * {
     *   "id": 1,
     *   "username": "johndoe",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "status": "ACTIVE",
     *   "createdAt": "2025-01-15T10:30:00Z",
     *   "lastLogin": "2025-01-16T08:15:00Z"
     * }
     * </pre>
     * <p>
     * <b>Error Responses:</b>
     * <ul>
     *   <li>401 Unauthorized: Missing or invalid JWT token</li>
     *   <li>404 Not Found: User no longer exists in database</li>
     *   <li>500 Internal Server Error: Unexpected server error</li>
     * </ul>
     * <p>
     * <b>Use Cases:</b>
     * <ul>
     *   <li>Display user profile information</li>
     *   <li>Verify user is still authenticated</li>
     *   <li>Check account status</li>
     *   <li>Audit logging (who performed an action)</li>
     * </ul>
     *
     * @param userDetails authenticated user details from JWT token
     * @return response entity with current user information
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("Current user request from: {}", userDetails.getUsername());

        CurrentUserResponse response = authService.getCurrentUser(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint to verify authentication service availability.
     * <p>
     * This is a simple endpoint that confirms the authentication service is running
     * and accepting requests. It's useful for:
     * <ul>
     *   <li>Monitoring and alerting systems</li>
     *   <li>Load balancer health checks</li>
     *   <li>Quick verification during deployment</li>
     *   <li>Testing API connectivity</li>
     * </ul>
     * <p>
     * <b>No Authentication Required:</b>
     * This endpoint is public and can be accessed without any credentials.
     * <p>
     * <b>Success Response (200 OK):</b>
     * <pre>
     * "Auth service is running"
     * </pre>
     *
     * @return response entity with health status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
