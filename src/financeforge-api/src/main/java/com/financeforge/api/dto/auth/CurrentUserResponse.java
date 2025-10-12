package com.financeforge.api.dto.auth;

import java.time.LocalDateTime;

/**
 * DTO for current user details endpoint response.
 * <p>
 * Extended user information for authenticated users accessing /auth/me endpoint.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */
public record CurrentUserResponse(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
