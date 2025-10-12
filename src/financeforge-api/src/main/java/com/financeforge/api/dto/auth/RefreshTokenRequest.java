package com.financeforge.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for JWT token refresh requests.
 * <p>
 * Used to obtain a new access token using a valid refresh token.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {
}
