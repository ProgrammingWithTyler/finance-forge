package com.financeforge.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response DTO for all API exceptions.
 * <p>
 * This record provides a consistent error response format across the entire API,
 * making it easier for clients to handle errors predictably. All error responses
 * follow this structure regardless of the underlying exception type.
 * <p>
 * <b>Design Principles:</b>
 * <ul>
 *   <li>Consistent structure - Same format for all errors</li>
 *   <li>User-friendly messages - Clear, actionable error descriptions</li>
 *   <li>No sensitive data - Never expose stack traces or internal details</li>
 *   <li>Machine-readable codes - Error codes for programmatic handling</li>
 *   <li>HTTP-aligned - Status codes match HTTP semantics</li>
 * </ul>
 * <p>
 * <b>Example Error Response:</b>
 * <pre>
 * {
 *   "timestamp": "2025-01-15T10:30:45",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character",
 *   "code": "VALIDATION_ERROR",
 *   "path": "/api/v1/auth/register",
 *   "details": {
 *     "field": "password",
 *     "rejectedValue": "weak"
 *   }
 * }
 * </pre>
 * <p>
 * <b>Error Codes:</b>
 * <ul>
 *   <li>VALIDATION_ERROR - Request validation failed (400)</li>
 *   <li>AUTHENTICATION_ERROR - Login credentials invalid (401)</li>
 *   <li>AUTHORIZATION_ERROR - Insufficient permissions (403)</li>
 *   <li>RESOURCE_NOT_FOUND - Requested resource doesn't exist (404)</li>
 *   <li>CONFLICT - Resource already exists or state conflict (409)</li>
 *   <li>INTERNAL_ERROR - Unexpected server error (500)</li>
 * </ul>
 *
 * @param timestamp when the error occurred (ISO 8601 format)
 * @param status HTTP status code (400, 401, 404, 500, etc.)
 * @param error HTTP status text ("Bad Request", "Unauthorized", etc.)
 * @param message human-readable error description for the client
 * @param code machine-readable error code for programmatic handling
 * @param path the API endpoint path where the error occurred
 * @param details optional additional error details (validation errors, field names, etc.)
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see GlobalExceptionHandler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't include null fields in JSON
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String code,
    String path,
    Map<String, Object> details
) {
    /**
     * Creates an ErrorResponse with minimal fields (no details).
     * <p>
     * Use this constructor for simple errors that don't need additional context.
     *
     * @param status HTTP status code
     * @param error HTTP status text
     * @param message error message
     * @param code error code
     * @param path request path
     */
    public ErrorResponse(int status, String error, String message, String code, String path) {
        this(LocalDateTime.now(), status, error, message, code, path, null);
    }

    /**
     * Creates an ErrorResponse with detailed error information.
     * <p>
     * Use this constructor when you need to provide additional context,
     * such as validation field errors or conflict details.
     *
     * @param status HTTP status code
     * @param error HTTP status text
     * @param message error message
     * @param code error code
     * @param path request path
     * @param details additional error details map
     */
    public ErrorResponse(int status, String error, String message, String code, String path, Map<String, Object> details) {
        this(LocalDateTime.now(), status, error, message, code, path, details);
    }
}
