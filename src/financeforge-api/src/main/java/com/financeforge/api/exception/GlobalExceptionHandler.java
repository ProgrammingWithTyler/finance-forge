package com.financeforge.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 * <p>
 * This class uses Spring's {@link RestControllerAdvice} to intercept exceptions thrown
 * by controllers and convert them into standardized {@link ErrorResponse} objects.
 * This ensures all API errors follow a consistent format and return appropriate HTTP status codes.
 * <p>
 * <b>How It Works:</b>
 * <pre>
 * 1. Exception thrown in controller or service
 * 2. Spring catches exception before it reaches client
 * 3. GlobalExceptionHandler finds matching @ExceptionHandler method
 * 4. Method converts exception to ErrorResponse
 * 5. Returns ResponseEntity with proper HTTP status
 * 6. Client receives clean, standardized error JSON
 * </pre>
 * <p>
 * <b>Why Global Exception Handling?</b>
 * <ul>
 *   <li>Consistency - All errors follow same format</li>
 *   <li>DRY Principle - No try-catch blocks in every controller</li>
 *   <li>Security - Prevents stack traces leaking to clients</li>
 *   <li>Maintainability - Error handling logic in one place</li>
 *   <li>Client-friendly - Clear, actionable error messages</li>
 * </ul>
 * <p>
 * <b>Exception Handling Strategy:</b>
 * <pre>
 * Authentication Errors (401):
 * - BadCredentialsException (wrong password)
 * - UsernameNotFoundException (user doesn't exist)
 * - IllegalStateException with "not active" (account status)
 *
 * Validation Errors (400):
 * - MethodArgumentNotValidException (@Valid annotation failures)
 * - IllegalArgumentException (business rule violations)
 *
 * Conflict Errors (409):
 * - DataIntegrityViolationException (duplicate username/email)
 * - IllegalStateException (username/email already taken)
 *
 * Not Found Errors (404):
 * - UsernameNotFoundException (for /me endpoint)
 *
 * Server Errors (500):
 * - DataAccessException (database errors)
 * - Generic Exception (unexpected errors)
 * </pre>
 * <p>
 * <b>Security Considerations:</b>
 * <ul>
 *   <li>Never expose stack traces to clients</li>
 *   <li>Use generic messages for authentication errors (prevent username enumeration)</li>
 *   <li>Log full exception details server-side for debugging</li>
 *   <li>Sanitize error messages to avoid sensitive data leaks</li>
 * </ul>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see ErrorResponse
 * @see RestControllerAdvice
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotations on request DTOs.
     * <p>
     * This exception is thrown when Spring's validation fails on a request body.
     * For example, when a RegisterRequest has an invalid email format or missing required field.
     * <p>
     * <b>Example Scenarios:</b>
     * <ul>
     *   <li>Email not in valid format (fails @Email validation)</li>
     *   <li>Username too short (fails @Size validation)</li>
     *   <li>Password blank (fails @NotBlank validation)</li>
     * </ul>
     * <p>
     * <b>Response Example:</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Validation failed for 2 fields",
     *   "code": "VALIDATION_ERROR",
     *   "path": "/api/v1/auth/register",
     *   "details": {
     *     "email": "Email must be valid",
     *     "password": "Password is required"
     *   }
     * }
     * </pre>
     *
     * @param ex the validation exception
     * @param request the HTTP request that caused the error
     * @return response entity with 400 status and validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        log.warn("Validation error on {}: {}", request.getRequestURI(), ex.getMessage());

        // Extract field errors from exception
        Map<String, Object> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Validation failed for " + fieldErrors.size() + "field(s)",
            "VALIDATION_ERROR",
            request.getRequestURI(),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles authentication failures (wrong username or password).
     * <p>
     * This exception is thrown by Spring Security when login credentials are invalid.
     * We return a generic message to prevent username enumeration attacks.
     * <p>
     * <b>Security Note:</b>
     * We intentionally use a generic message like "Invalid username or password"
     * instead of "Username not found" or "Wrong password" to prevent attackers
     * from discovering valid usernames.
     * <p>
     * <b>Response Example:</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 401,
     *   "error": "Unauthorized",
     *   "message": "Invalid username or password",
     *   "code": "AUTHENTICATION_ERROR",
     *   "path": "/api/v1/auth/login"
     * }
     * </pre>
     *
     * @param ex the bad credentials exception
     * @param request the HTTP request that caused the error
     * @return response entity with 401 status and generic error message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
        BadCredentialsException ex,
        HttpServletRequest request
    ) {
        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            "Invalid username or password", // Generic message for security
            "AUTHENTICATION_ERROR",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles user not found exceptions.
     * <p>
     * This exception is thrown when:
     * <ul>
     *   <li>User tries to log in with non-existent username</li>
     *   <li>JWT token references a deleted user</li>
     *   <li>GET /auth/me called but user was deleted</li>
     * </ul>
     * <p>
     * <b>Context Matters:</b>
     * - For login: Return 401 with generic message (security)
     * - For /me endpoint: Return 404 (user was deleted after login)
     * <p>
     * <b>Response Example (404):</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 404,
     *   "error": "Not Found",
     *   "message": "User not found",
     *   "code": "RESOURCE_NOT_FOUND",
     *   "path": "/api/v1/auth/me"
     * }
     * </pre>
     *
     * @param ex the username not found exception
     * @param request the HTTP request that caused the error
     * @return response entity with 404 status for /me, 401 for login
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
        UsernameNotFoundException ex,
        HttpServletRequest request
    ) {
        log.warn("User not found failed on {}: {}", request.getRequestURI(), ex.getMessage());

        // For login endpoint, return 401 with generic message
        if (request.getRequestURI().contains("/login")) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid username or password", // Generic message for security
                "AUTHENTICATION_ERROR",
                request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // For other endpoints (like /me), return 404
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            "User not found",
            "RESOURCE_NOT_FOUND",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles database constraint violations (duplicate username/email).
     * <p>
     * This exception is thrown when trying to insert data that violates unique constraints.
     * Common scenarios:
     * <ul>
     *   <li>Registering with existing username</li>
     *   <li>Registering with existing email</li>
     *   <li>Updating user email to one that's already taken</li>
     * </ul>
     * <p>
     * <b>Response Example:</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 409,
     *   "error": "Conflict",
     *   "message": "Username 'johndoe' is already taken",
     *   "code": "CONFLICT",
     *   "path": "/api/v1/auth/register"
     * }
     * </pre>
     *
     * @param ex the data integrity violation exception
     * @param request the HTTP request that caused the error
     * @return response entity with 409 status and conflict details
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());

        // Extract user-friendly message from exception
        String message = ex.getMessage();
        if (message != null && (message.contains("already taken") || message.contains("already contained"))) {
            // Use the friendly message from USER_MGMT_PKG
            message = extractFriendlyMessage(message);
        } else {
            message = "A conflict occurred with existing data";
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            message,
            "CONFLICT",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles illegal argument exceptions (business rule violations).
     * <p>
     * These exceptions are thrown when business logic validation fails:
     * <ul>
     *   <li>Password doesn't meet complexity requirements</li>
     *   <li>Invalid token type (access token used as refresh)</li>
     *   <li>Invalid date ranges or amounts</li>
     * </ul>
     * <p>
     * <b>Response Example:</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Password must contain at least one uppercase letter...",
     *   "code": "VALIDATION_ERROR",
     *   "path": "/api/v1/auth/register"
     * }
     * </pre>
     *
     * @param ex the illegal argument exception
     * @param request the HTTP request that caused the error
     * @return response entity with 400 status and validation error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            "VALIDATION_ERROR",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles illegal state exceptions (state conflicts).
     * <p>
     * These exceptions are thrown when an operation can't be performed due to current state:
     * <ul>
     *   <li>Account not active (status is PENDING or INACTIVE)</li>
     *   <li>Username already taken</li>
     *   <li>Email already registered</li>
     *   <li>Invalid refresh token</li>
     * </ul>
     * <p>
     * <b>Response Mapping:</b>
     * - "already taken" / "already registered" → 409 Conflict
     * - "not active" → 401 Unauthorized
     * - Everything else → 400 Bad Request
     *
     * @param ex the illegal state exception
     * @param request the HTTP request that caused the error
     * @return response entity with appropriate status code
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
        IllegalStateException ex,
        HttpServletRequest request
    ) {
        log.warn("Illegal state on {}: {}", request.getRequestURI(), ex.getMessage());

        // Determine appropriate status code based on message
        HttpStatus status;
        String code;

        if (ex.getMessage().contains("already taken") || ex.getMessage().contains("already registered")) {
            status = HttpStatus.CONFLICT;
            code = "CONFLICT";
        } else if (ex.getMessage().contains("not active")) {
            status = HttpStatus.UNAUTHORIZED;
            code = "AUTHENTICATION_ERROR";
        } else {
            status = HttpStatus.BAD_REQUEST;
            code = "VALIDATION_ERROR";
        }

        ErrorResponse error = new ErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            ex.getMessage(),
            code,
            request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handles database access exceptions (PL/SQL errors, connection issues).
     * <p>
     * These exceptions indicate problems with database operations:
     * <ul>
     *   <li>PL/SQL procedure errors (from USER_MGMT_PKG)</li>
     *   <li>Database connection failures</li>
     *   <li>SQL syntax errors</li>
     *   <li>Transaction failures</li>
     * </ul>
     * <p>
     * <b>Response Example:</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 500,
     *   "error": "Internal Server Error",
     *   "message": "A database error occurred. Please try again later.",
     *   "code": "INTERNAL_ERROR",
     *   "path": "/api/v1/auth/register"
     * }
     * </pre>
     *
     * @param ex the data access exception
     * @param request the HTTP request that caused the error
     * @return response entity with 500 status
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
        DataAccessException ex,
        HttpServletRequest request
    ) {
        log.error("Database error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "A database error occurred. Please try again later.",
            "INTERNAL_ERROR",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles all other uncaught exceptions.
     * <p>
     * This is the catch-all handler for any unexpected errors that don't match
     * the specific handlers above. It ensures the client always receives a
     * clean error response instead of a stack trace.
     * <p>
     * <b>Security Note:</b>
     * We log the full exception server-side but return a generic message to the client
     * to avoid leaking implementation details.
     * <p>
     * <b>Response Example:</b>
     * <pre>
     * {
     *   "timestamp": "2025-01-15T10:30:45",
     *   "status": 500,
     *   "error": "Internal Server Error",
     *   "message": "An unexpected error occurred. Please try again later.",
     *   "code": "INTERNAL_ERROR",
     *   "path": "/api/v1/auth/register"
     * }
     * </pre>
     *
     * @param ex the unexpected exception
     * @param request the HTTP request that caused the error
     * @return response entity with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            "INTERNAL_ERROR",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Extracts user-friendly error messages from PL/SQL exceptions.
     * <p>
     * Our stored procedures return messages like:
     * "Username 'johndoe' is already taken" or "Email 'john@example.com' is already registered"
     * <p>
     * This method extracts these clean messages from the exception stack.
     *
     * @param fullMessage the full exception message
     * @return the user-friendly portion of the message
     */
    private String extractFriendlyMessage(String fullMessage) {
        // Look for messages between quotes or after "Error: "
        if (fullMessage.contains("Error: ")) {
            return fullMessage.substring(fullMessage.indexOf("Error: ") + 7).trim();
        }
        return fullMessage;
    }
}
