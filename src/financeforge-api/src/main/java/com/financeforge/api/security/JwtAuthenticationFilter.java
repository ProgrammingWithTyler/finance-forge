package com.financeforge.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that validates JWT tokens on every request.
 * <p>
 * This filter extends {@link OncePerRequestFilter} to ensure it executes exactly once per request.
 * It intercepts all incoming HTTP requests and performs JWT token validation before the request
 * reaches the controller layer.
 * <p>
 * <b>Filter Execution Flow:</b>
 * <pre>
 * 1. Extract JWT token from Authorization header
 * 2. Validate "Bearer " prefix
 * 3. Extract username from token
 * 4. Check if user is already authenticated (skip if yes)
 * 5. Load user details from database
 * 6. Validate token (signature + expiration + user match)
 * 7. If valid: Set authentication in SecurityContext
 * 8. Continue filter chain to next filter/controller
 *
 * If token is invalid/missing:
 * - Public endpoints: Request continues (SecurityConfig allows)
 * - Protected endpoints: Spring Security returns 401 Unauthorized
 * </pre>
 * <p>
 * <b>Request Flow Example:</b>
 * <pre>
 * GET /api/v1/expenses
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *
 * ↓
 * JwtAuthenticationFilter (this class)
 * ├─ Extract token
 * ├─ Validate token
 * ├─ Load user from DB
 * └─ Set SecurityContext
 *
 * ↓
 * Spring Security checks authorization rules
 * ├─ Is user authenticated? YES ✓
 * └─ Does user have required role? YES ✓
 *
 * ↓
 * ExpenseController.getExpenses() executes
 * </pre>
 * <p>
 * <b>SecurityContext Explained:</b>
 * The SecurityContext is Spring Security's way of knowing "who is logged in" for this request.
 * We set it in this filter so that:
 * <ul>
 *   <li>Controllers can access current user: @AuthenticationPrincipal UserDetails</li>
 *   <li>Spring Security can enforce authorization rules</li>
 *   <li>Audit logs can track who performed actions</li>
 * </ul>
 * <p>
 * <b>Why OncePerRequestFilter?</b>
 * <ul>
 *   <li>Guarantees filter runs exactly once per request</li>
 *   <li>Handles forward/include scenarios correctly</li>
 *   <li>Spring Boot best practice for authentication filters</li>
 * </ul>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see JwtService
 * @see CustomUserDetailsService
 * @see OncePerRequestFilter
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Constructs a new JWT authentication filter.
     *
     * @param jwtService service for JWT token operations
     * @param userDetailsService service for loading user details
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Filters incoming HTTP requests to validate JWT tokens.
     * <p>
     * This method is called by Spring Security's filter chain for every HTTP request.
     * It performs the complete JWT validation flow and sets authentication in the
     * SecurityContext if the token is valid.
     * <p>
     * <b>Implementation Steps:</b>
     * <ol>
     *   <li>Extract Authorization header from request</li>
     *   <li>Check if header starts with "Bearer "</li>
     *   <li>Extract JWT token (remove "Bearer " prefix)</li>
     *   <li>Extract username from token payload</li>
     *   <li>Check if user is already authenticated (avoid redundant DB calls)</li>
     *   <li>Load UserDetails from database via CustomUserDetailsService</li>
     *   <li>Validate token (signature, expiration, username match)</li>
     *   <li>Create Authentication object and set in SecurityContext</li>
     *   <li>Continue filter chain</li>
     * </ol>
     * <p>
     * <b>Error Handling:</b>
     * If any step fails (invalid token, expired token, user not found), the method
     * simply continues the filter chain WITHOUT setting authentication. Spring Security
     * will then handle the unauthenticated request according to SecurityConfig rules.
     * <p>
     * <b>Performance Note:</b>
     * We check if authentication already exists to avoid redundant database queries
     * if the filter somehow runs multiple times for the same request.
     *
     * @param request the HTTP request being filtered
     * @param response the HTTP response (not modified by this filter)
     * @param filterChain the filter chain to continue execution
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract Authorization header
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // If no Authorization header or doesn't start with "Bearer ", skip filter
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No JWT token found in request to {}", request.getRequestURI());
            filterChain.doFilter(request, response);
        }

        try {
            // 2. Extract JWT token (remove "Bearer " prefix)
            final String jwt = authHeader.substring(BEARER_PREFIX.length());

            // 3. Extract username token
            final String username = jwtService.extractUsername(jwt);

            log.debug("JWT token found for user: {} ", username);

            // 4. Check if user is already authenticated (avoid redundant processing)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Load user details from database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 6. Validate token (signature + expiration + username match)
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // 7. Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // credentials (we don't need password here)
                        userDetails.getAuthorities()
                    );

                    // Set requeste details (IP address, session ID, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 8. Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("User '{}' authenticated successfully via JWT", username);
                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }
        } catch (Exception e) {
            // Log error but don't stop request processing
            // Spring Security will handle unauthenticated request appropriately
            log.error("JWT authentication failed: {}", e.getMessage());
        }

        // 9. Continue filter chain (pass request to next filter or controller)
        filterChain.doFilter(request, response);
    }
}
