package com.financeforge.api.config;

import com.financeforge.api.security.CustomUserDetailsService;
import com.financeforge.api.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for JWT-based authentication.
 * <p>
 * This configuration class sets up the complete security infrastructure for the application:
 * <ul>
 *   <li>JWT authentication filter chain</li>
 *   <li>Public vs protected endpoint rules</li>
 *   <li>BCrypt password encoding</li>
 *   <li>Custom UserDetailsService integration</li>
 *   <li>Stateless session management</li>
 *   <li>CORS configuration</li>
 *   <li>CSRF protection (disabled for stateless JWT)</li>
 * </ul>
 * <p>
 * <b>Security Architecture:</b>
 * <pre>
 * Request Flow:
 * 1. Client sends request with Authorization: Bearer <token>
 * 2. JwtAuthenticationFilter intercepts request
 * 3. Filter extracts and validates JWT token
 * 4. If valid, sets Authentication in SecurityContext
 * 5. Request proceeds to controller
 * 6. Controller executes with authenticated user
 * 7. Response returns to client
 *
 * If token invalid/missing:
 * - Public endpoints: Request proceeds normally
 * - Protected endpoints: 401 Unauthorized returned
 * </pre>
 * <p>
 * <b>Endpoint Security Rules:</b>
 * <pre>
 * Public Endpoints (No Authentication Required):
 * - POST /api/v1/auth/register  (User registration)
 * - POST /api/v1/auth/login     (User login)
 * - POST /api/v1/auth/refresh   (Token refresh)
 * - GET  /api/v1/auth/health    (Health check)
 * - /swagger-ui/**              (API documentation)
 * - /api-docs/**                (OpenAPI specs)
 *
 * Protected Endpoints (Authentication Required):
 * - GET  /api/v1/auth/me        (Current user info)
 * - All /api/v1/categories/**   (Category management)
 * - All /api/v1/expenses/**     (Expense management)
 * - All /api/v1/reports/**      (Analytics/reporting)
 * </pre>
 * <p>
 * <b>Why Stateless (No Sessions)?</b>
 * <ul>
 *   <li>JWT tokens contain all user info - no server-side session storage needed</li>
 *   <li>Better scalability - no session replication across servers</li>
 *   <li>Mobile-friendly - no cookie dependencies</li>
 *   <li>Microservice-ready - services can validate tokens independently</li>
 * </ul>
 * <p>
 * <b>Why CSRF Disabled?</b>
 * <ul>
 *   <li>CSRF attacks rely on cookies - we use JWT in headers instead</li>
 *   <li>Stateless JWT architecture doesn't need CSRF protection</li>
 *   <li>JWT tokens are explicitly sent, not automatically (like cookies)</li>
 * </ul>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see JwtAuthenticationFilter
 * @see CustomUserDetailsService
 * @see JwtService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured annotations
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Constructs the security configuration with required dependencies.
     *
     * @param jwtAuthenticationFilter filter for JWT token validation
     * @param userDetailsService service for loading user details
     */
    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        CustomUserDetailsService userDetailsService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Configures the security filter chain.
     * <p>
     * This is the main security configuration that defines:
     * <ul>
     *   <li>Which endpoints require authentication</li>
     *   <li>Filter chain order (JWT filter before authentication filter)</li>
     *   <li>Session management policy (stateless)</li>
     *   <li>CORS and CSRF settings</li>
     * </ul>
     * <p>
     * <b>Filter Chain Order:</b>
     * <pre>
     * 1. CorsFilter (handles CORS preflight)
     * 2. JwtAuthenticationFilter (validates JWT, sets Authentication)
     * 3. UsernamePasswordAuthenticationFilter (Spring Security default)
     * 4. ... other security filters
     * 5. Controller (if authorized)
     * </pre>
     * <p>
     * <b>Authorization Rules (evaluated top to bottom):</b>
     * <ol>
     *   <li>Public auth endpoints - permitAll()</li>
     *   <li>Swagger/API docs - permitAll()</li>
     *   <li>Everything else - authenticated()</li>
     * </ol>
     *
     * @param http the HttpSecurity to configure
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF (we're using JWT, not cookies)
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/health"
                ).permitAll()

                // Swagger UI and API docs (public for development)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Configure session management (stateless for JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Set custom authentication provider
            .authenticationProvider(authenticationProvider())

            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }

    /**
     * Creates the password encoder bean using BCrypt.
     * <p>
     * BCrypt is the industry-standard password hashing algorithm:
     * <ul>
     *   <li>Automatically salts passwords (prevents rainbow table attacks)</li>
     *   <li>Configurable work factor (12 rounds = good balance)</li>
     *   <li>Designed to be slow (prevents brute force attacks)</li>
     *   <li>One-way hash (cannot be reversed)</li>
     * </ul>
     * <p>
     * <b>BCrypt Work Factor:</b>
     * <pre>
     * - Default: 10 rounds (1024 iterations)
     * - We use: 12 rounds (4096 iterations) - more secure
     * - Each increment doubles computation time
     * - 12 rounds ≈ 250ms to hash a password (good for security)
     * </pre>
     * <p>
     * <b>How It's Used:</b>
     * <pre>
     * Registration:
     * String hash = passwordEncoder.encode("MyPassword123!");
     * // Returns: $2a$12$randomSalt...hashedPassword
     *
     * Login:
     * boolean matches = passwordEncoder.matches("MyPassword123!", storedHash);
     * // Returns: true if password matches, false otherwise
     * </pre>
     *
     * @return BCrypt password encoder with 12 rounds
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Creates the authentication provider that uses our UserDetailsService and password encoder.
     * <p>
     * The DaoAuthenticationProvider is Spring Security's default authentication provider.
     * It performs the following steps during login:
     * <ol>
     *   <li>Calls userDetailsService.loadUserByUsername(username)</li>
     *   <li>Gets UserDetails with password hash from database</li>
     *   <li>Uses passwordEncoder.matches(plaintext, hash) to verify password</li>
     *   <li>If match: returns authenticated Authentication object</li>
     *   <li>If no match: throws BadCredentialsException</li>
     * </ol>
     * <p>
     * <b>Note on Deprecation Warnings:</b>
     * Spring Security 6.x shows deprecation warnings on DaoAuthenticationProvider constructor
     * and setters, but the recommended replacement (constructor with both parameters) doesn't
     * exist in all versions yet. This is the current standard approach until Spring Security 7.
     * <p>
     * <b>Why Separate AuthenticationProvider?</b>
     * <ul>
     *   <li>Decouples authentication logic from security config</li>
     *   <li>Can be reused across multiple security configs</li>
     *   <li>Easy to swap implementations (LDAP, OAuth, etc.)</li>
     *   <li>Follows Spring Security best practices</li>
     * </ul>
     *
     * @return configured authentication provider
     */
    @Bean
    @SuppressWarnings("deprecation")  // Suppress until Spring Security 7 provides alternative
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the authentication manager as a bean.
     * <p>
     * The AuthenticationManager is the main Spring Security interface for authentication.
     * It's used by:
     * <ul>
     *   <li>AuthService.login() - to authenticate username/password</li>
     *   <li>JwtAuthenticationFilter - to set authentication in SecurityContext</li>
     *   <li>Any custom authentication logic</li>
     * </ul>
     * <p>
     * <b>How It Works:</b>
     * <pre>
     * AuthenticationManager authManager = ...;
     *
     * // Create authentication request
     * Authentication request = new UsernamePasswordAuthenticationToken(
     *     "username",
     *     "password"
     * );
     *
     * // Authenticate (calls AuthenticationProvider internally)
     * Authentication result = authManager.authenticate(request);
     *
     * // If successful, result contains authenticated user details
     * // If failed, throws AuthenticationException
     * </pre>
     *
     * @param config the authentication configuration
     * @return the authentication manager
     * @throws Exception if authentication manager cannot be created
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     * <p>
     * CORS is required when your frontend runs on a different domain/port than the backend.
     * For example:
     * <ul>
     *   <li>Frontend: http://localhost:3000 (React dev server)</li>
     *   <li>Backend: http://localhost:8080 (Spring Boot)</li>
     * </ul>
     * <p>
     * <b>Current Configuration:</b>
     * <pre>
     * Allowed Origins: http://localhost:3000, http://localhost:4200
     * Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
     * Allowed Headers: Authorization, Content-Type, Accept
     * Exposed Headers: Authorization (for response tokens)
     * Max Age: 3600 seconds (1 hour)
     * Allow Credentials: true (for cookies, if needed later)
     * </pre>
     * <p>
     * <b>Security Notes:</b>
     * <ul>
     *   <li>Development: Allow localhost origins</li>
     *   <li>Production: Restrict to actual frontend domain (e.g., https://financeforge.com)</li>
     *   <li>Never use "*" (wildcard) in production with credentials</li>
     * </ul>
     * <p>
     * <b>Production Configuration:</b>
     * <pre>
     * // In application-prod.yml:
     * cors:
     *   allowed-origins: https://financeforge.com
     *
     * // Then inject it:
     * @Value("${cors.allowed-origins}")
     * private String allowedOrigins;
     * </pre>
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow requests from these origins
        // TODO: Update for production with actual frontend URL
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",  // React default port
            "http://localhost:4200"   // Angular default port
        ));

        // Allow these HTTP methods
        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS"
        ));

        // Allow these headers in requests
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With"
        ));

        // Expose these headers in responses (so frontend can read them)
        configuration.setExposedHeaders(List.of(
            "Authorization"
        ));

        // Allow cookies/credentials (needed if you use refresh tokens in cookies)
        configuration.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        // Apply CORS config to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}