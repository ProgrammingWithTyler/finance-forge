package com.financeforge.api.domain.model.enums;

/**
 * User account status enumeration.
 * Must match Oracle CHECK constraint: ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING'))
 *
 * CRITICAL: Do not add new statuses without updating the database constraint first.
 */
public enum UserStatus {
    /**
     * User account is active and can log in.
     */
    ACTIVE,

    /**
     * User account is inactive (disabled by admin or user).
     * Cannot log in until reactivated.
     */
    INACTIVE,

    /**
     * User account is pending email verification.
     * Cannot log in until email is confirmed.
     */
    PENDING
}