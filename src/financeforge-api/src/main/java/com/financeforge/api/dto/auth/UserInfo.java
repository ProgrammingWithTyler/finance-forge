package com.financeforge.api.dto.auth;

/**
 * DTO for user information included in auth responses.
 * <p>
 * Contains non-sensitive user data that can be safely exposed to clients.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */
public record UserInfo(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName
) {
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
