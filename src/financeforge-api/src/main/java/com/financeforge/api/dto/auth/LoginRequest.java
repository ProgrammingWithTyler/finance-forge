package com.financeforge.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login requests.
 * <p>
 * Contains credentials for authentication. Can accept either username or email.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */

public record LoginRequest(
    @NotBlank(message = "Username or email is required")
    String usernameOrEmail,

    @NotBlank(message = "Password is required")
    String password
) {
}
