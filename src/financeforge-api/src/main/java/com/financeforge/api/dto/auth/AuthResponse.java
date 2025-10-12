package com.financeforge.api.dto.auth;

/**
 * DTO for authentication responses.
 * <p>
 * Contains JWT tokens and user information returned after successful authentication.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {
    /**
     * Creates an AuthResponse with default token type "Bearer".
     */
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
