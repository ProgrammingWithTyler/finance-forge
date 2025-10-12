-- V005__create_user_mgmt_pkg.sql
-- Author: ProgrammingWithTyler
-- Created: 2025-10-08
-- Purpose: PL/SQL package for CRUD operations on USERS table
-- Notes:
--   - Package name: USER_MGMT_PKG (consistent with LOG_MGMT_PKG naming)
--   - All procedures use consistent cursor parameter: p_cursor
--   - Includes audit logging integration via LOG_MGMT_PKG
--   - Human-readable error messages via o_err_msg OUT parameter

-- =======================================================
-- 1. PACKAGE SPEC
-- =======================================================
CREATE OR REPLACE PACKAGE USER_MGMT_PKG AS

    ------------------------------------------------------------------------
    -- CREATE_USER
    -- Inserts a new user and returns the generated ID
    -- Logs to audit_log via LOG_MGMT_PKG
    ------------------------------------------------------------------------
    PROCEDURE CREATE_USER(
        p_username      IN  VARCHAR2,
        p_email         IN  VARCHAR2,
        p_password_hash IN  VARCHAR2,
        p_first_name    IN  VARCHAR2,
        p_last_name     IN  VARCHAR2,
        p_status        IN  VARCHAR2 DEFAULT 'PENDING',
        p_created_by    IN  VARCHAR2 DEFAULT 'SYSTEM',
        p_user_id       OUT NUMBER,
        o_err_msg       OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- GET_USER_BY_ID
    -- Returns a single user by ID as SYS_REFCURSOR
    ------------------------------------------------------------------------
    PROCEDURE GET_USER_BY_ID(
        p_user_id IN  NUMBER,
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- GET_BY_EMAIL
    -- Returns a single user by email as SYS_REFCURSOR
    ------------------------------------------------------------------------
    PROCEDURE GET_BY_EMAIL(
        p_email   IN  VARCHAR2,
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- FIND_BY_USERNAME
    -- Returns a single user by username as SYS_REFCURSOR
    ------------------------------------------------------------------------
    PROCEDURE FIND_BY_USERNAME(
        p_username IN  VARCHAR2,
        p_cursor   OUT SYS_REFCURSOR,
        o_err_msg  OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- FIND_ALL_ACTIVE_USERS
    -- Returns all users with STATUS = 'ACTIVE' as SYS_REFCURSOR
    ------------------------------------------------------------------------
    PROCEDURE FIND_ALL_ACTIVE_USERS(
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- FIND_BY_STATUS
    -- Returns all users with matching status as SYS_REFCURSOR
    ------------------------------------------------------------------------
    PROCEDURE FIND_BY_STATUS(
        p_status  IN  VARCHAR2,
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- UPDATE_USER
    -- Updates an existing user's details with audit logging
    ------------------------------------------------------------------------
    PROCEDURE UPDATE_USER(
        p_user_id    IN  NUMBER,
        p_email      IN  VARCHAR2,
        p_first_name IN  VARCHAR2,
        p_last_name  IN  VARCHAR2,
        p_status     IN  VARCHAR2,
        p_updated_by IN  VARCHAR2 DEFAULT 'SYSTEM',
        o_err_msg    OUT VARCHAR2
    );

    ------------------------------------------------------------------------
    -- DELETE_USER (Soft Delete)
    -- Sets user status to INACTIVE instead of physical delete
    ------------------------------------------------------------------------
    PROCEDURE DELETE_USER(
        p_user_id    IN  NUMBER,
        p_deleted_by IN  VARCHAR2 DEFAULT 'SYSTEM',
        o_err_msg    OUT VARCHAR2
    );

END USER_MGMT_PKG;
/

-- =======================================================
-- 2. PACKAGE BODY
-- =======================================================
CREATE OR REPLACE PACKAGE BODY USER_MGMT_PKG AS

    FUNCTION username_exists(p_username VARCHAR2) RETURN BOOLEAN IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
        FROM USERS
        WHERE USERNAME = p_username;
        RETURN v_count > 0;
    END username_exists;

    FUNCTION email_exists(p_email VARCHAR2) RETURN BOOLEAN IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
        FROM USERS
        WHERE EMAIL = p_email;
        RETURN v_count > 0;
    END email_exists;

    ------------------------------------------------------------------------
    -- CREATE_USER implementation
    -- FIXED: Now sets UPDATED_BY = CREATED_BY on insert for proper audit trail
    ------------------------------------------------------------------------
    PROCEDURE CREATE_USER(
        p_username      IN  VARCHAR2,
        p_email         IN  VARCHAR2,
        p_password_hash IN  VARCHAR2,
        p_first_name    IN  VARCHAR2,
        p_last_name     IN  VARCHAR2,
        p_status        IN  VARCHAR2 DEFAULT 'PENDING',
        p_created_by    IN  VARCHAR2 DEFAULT 'SYSTEM',
        p_user_id       OUT NUMBER,
        o_err_msg       OUT VARCHAR2
    ) IS
        v_log_id NUMBER;
        v_audit_err VARCHAR2(4000);
    BEGIN
        IF p_username IS NULL OR TRIM(p_username) IS NULL THEN
            o_err_msg := 'Username cannot be null or empty';
            RETURN;
        END IF;

        IF p_email IS NULL OR TRIM(p_email) IS NULL THEN
            o_err_msg := 'Email cannot be null or empty';
            RETURN;
        END IF;

        IF p_password_hash IS NULL OR TRIM(p_password_hash) IS NULL THEN
            o_err_msg := 'Password hash cannot be null or empty';
            RETURN;
        END IF;

        IF username_exists(p_username) THEN
            o_err_msg := 'Username "' || p_username || '" is already taken';
            RETURN;
        END IF;

        IF email_exists(p_email) THEN
            o_err_msg := 'Email "' || p_email || '" is already registered';
            RETURN;
        END IF;

        -- FIXED: Explicitly set UPDATED_BY to match CREATED_BY
        INSERT INTO USERS (
            USERNAME, EMAIL, PASSWORD_HASH, FIRST_NAME, LAST_NAME,
            STATUS, CREATED_BY, UPDATED_BY
        ) VALUES (
            p_username, p_email, p_password_hash, p_first_name, p_last_name,
            p_status, p_created_by, p_created_by
        )
        RETURNING ID INTO p_user_id;

        LOG_MGMT_PKG.CREATE_LOG_ENTRY(
            p_entity_name => 'USERS',
            p_entity_id => p_user_id,
            p_action_type => 'INSERT',
            p_action_by => p_created_by,
            p_description => 'Created user: ' || p_username || ' (' || p_email || ')',
            p_severity => 'INFO',
            p_log_id => v_log_id,
            o_err_msg => v_audit_err
        );

        COMMIT;
        o_err_msg := NULL;

    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            ROLLBACK;
            o_err_msg := 'Duplicate username or email detected';
        WHEN OTHERS THEN
            ROLLBACK;
            o_err_msg := 'Error creating user: ' || SQLERRM;
    END CREATE_USER;

    PROCEDURE GET_USER_BY_ID(
        p_user_id IN  NUMBER,
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    ) IS
    BEGIN
        OPEN p_cursor FOR
            SELECT
                ID, USERNAME, EMAIL, PASSWORD_HASH, FIRST_NAME, LAST_NAME,
                STATUS, CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
            FROM USERS
            WHERE ID = p_user_id;
        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving user by ID: ' || SQLERRM;
    END GET_USER_BY_ID;

    PROCEDURE GET_BY_EMAIL(
        p_email   IN  VARCHAR2,
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    ) IS
    BEGIN
        OPEN p_cursor FOR
            SELECT
                ID, USERNAME, EMAIL, PASSWORD_HASH, FIRST_NAME, LAST_NAME,
                STATUS, CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
            FROM USERS
            WHERE EMAIL = p_email;
        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving user by email: ' || SQLERRM;
    END GET_BY_EMAIL;

    PROCEDURE FIND_BY_USERNAME(
        p_username IN  VARCHAR2,
        p_cursor   OUT SYS_REFCURSOR,
        o_err_msg  OUT VARCHAR2
    ) IS
    BEGIN
        OPEN p_cursor FOR
            SELECT
                ID, USERNAME, EMAIL, PASSWORD_HASH, FIRST_NAME, LAST_NAME,
                STATUS, CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
            FROM USERS
            WHERE USERNAME = p_username;
        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving user by username: ' || SQLERRM;
    END FIND_BY_USERNAME;

    PROCEDURE FIND_ALL_ACTIVE_USERS(
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    ) IS
    BEGIN
        OPEN p_cursor FOR
            SELECT
                ID, USERNAME, EMAIL, PASSWORD_HASH, FIRST_NAME, LAST_NAME,
                STATUS, CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
            FROM USERS
            WHERE STATUS = 'ACTIVE'
            ORDER BY USERNAME;
        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving active users: ' || SQLERRM;
    END FIND_ALL_ACTIVE_USERS;

    PROCEDURE FIND_BY_STATUS(
        p_status  IN  VARCHAR2,
        p_cursor  OUT SYS_REFCURSOR,
        o_err_msg OUT VARCHAR2
    ) IS
    BEGIN
        IF p_status NOT IN ('ACTIVE', 'INACTIVE', 'PENDING') THEN
            o_err_msg := 'Invalid status. Must be ACTIVE, INACTIVE, or PENDING';
            RETURN;
        END IF;

        OPEN p_cursor FOR
            SELECT
                ID, USERNAME, EMAIL, PASSWORD_HASH, FIRST_NAME, LAST_NAME,
                STATUS, CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY
            FROM USERS
            WHERE STATUS = p_status
            ORDER BY USERNAME;
        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving users by status: ' || SQLERRM;
    END FIND_BY_STATUS;

    PROCEDURE UPDATE_USER(
        p_user_id    IN  NUMBER,
        p_email      IN  VARCHAR2,
        p_first_name IN  VARCHAR2,
        p_last_name  IN  VARCHAR2,
        p_status     IN  VARCHAR2,
        p_updated_by IN  VARCHAR2 DEFAULT 'SYSTEM',
        o_err_msg    OUT VARCHAR2
    ) IS
        v_old_email VARCHAR2(255);
        v_old_fname VARCHAR2(100);
        v_old_lname VARCHAR2(100);
        v_old_status VARCHAR2(50);
        v_username VARCHAR2(100);
        v_log_id NUMBER;
        v_audit_err VARCHAR2(4000);
        v_changes VARCHAR2(4000);
    BEGIN
        IF p_status NOT IN ('ACTIVE', 'INACTIVE', 'PENDING') THEN
            o_err_msg := 'Invalid status. Must be ACTIVE, INACTIVE, or PENDING';
            RETURN;
        END IF;

        BEGIN
            SELECT EMAIL, FIRST_NAME, LAST_NAME, STATUS, USERNAME
            INTO v_old_email, v_old_fname, v_old_lname, v_old_status, v_username
            FROM USERS
            WHERE ID = p_user_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                o_err_msg := 'No user found with ID: ' || p_user_id;
                RETURN;
        END;

        IF p_email != v_old_email THEN
            DECLARE
                v_email_count NUMBER;
            BEGIN
                SELECT COUNT(*) INTO v_email_count
                FROM USERS
                WHERE EMAIL = p_email AND ID != p_user_id;
                IF v_email_count > 0 THEN
                    o_err_msg := 'Email "' || p_email || '" is already registered to another user';
                    RETURN;
                END IF;
            END;
        END IF;

        UPDATE USERS
        SET EMAIL       = p_email,
            FIRST_NAME  = p_first_name,
            LAST_NAME   = p_last_name,
            STATUS      = p_status,
            UPDATED_BY  = p_updated_by,
            UPDATED_AT  = CURRENT_TIMESTAMP
        WHERE ID = p_user_id;

        IF SQL%ROWCOUNT = 0 THEN
            o_err_msg := 'Failed to update user with ID: ' || p_user_id;
            RETURN;
        END IF;

        v_changes := 'Updated user: ' || v_username;
        IF p_email != v_old_email THEN
            v_changes := v_changes || ' | Email: ' || v_old_email || ' -> ' || p_email;
        END IF;
        IF p_status != v_old_status THEN
            v_changes := v_changes || ' | Status: ' || v_old_status || ' -> ' || p_status;
        END IF;

        LOG_MGMT_PKG.CREATE_LOG_ENTRY(
            p_entity_name => 'USERS',
            p_entity_id => p_user_id,
            p_action_type => 'UPDATE',
            p_action_by => p_updated_by,
            p_description => v_changes,
            p_severity => 'INFO',
            p_log_id => v_log_id,
            o_err_msg => v_audit_err
        );

        COMMIT;
        o_err_msg := NULL;

    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            ROLLBACK;
            o_err_msg := 'Duplicate email detected during update';
        WHEN OTHERS THEN
            ROLLBACK;
            o_err_msg := 'Error updating user: ' || SQLERRM;
    END UPDATE_USER;

    PROCEDURE DELETE_USER(
        p_user_id    IN  NUMBER,
        p_deleted_by IN  VARCHAR2 DEFAULT 'SYSTEM',
        o_err_msg    OUT VARCHAR2
    ) IS
        v_username VARCHAR2(100);
        v_log_id NUMBER;
        v_audit_err VARCHAR2(4000);
    BEGIN
        BEGIN
            SELECT USERNAME INTO v_username
            FROM USERS
            WHERE ID = p_user_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                o_err_msg := 'No user found with ID: ' || p_user_id;
                RETURN;
        END;

        UPDATE USERS
        SET STATUS = 'INACTIVE',
            UPDATED_BY = p_deleted_by,
            UPDATED_AT = CURRENT_TIMESTAMP
        WHERE ID = p_user_id;

        IF SQL%ROWCOUNT = 0 THEN
            o_err_msg := 'Failed to delete user with ID: ' || p_user_id;
            RETURN;
        END IF;

        LOG_MGMT_PKG.CREATE_LOG_ENTRY(
            p_entity_name => 'USERS',
            p_entity_id => p_user_id,
            p_action_type => 'DELETE',
            p_action_by => p_deleted_by,
            p_description => 'Soft deleted user: ' || v_username,
            p_severity => 'WARNING',
            p_log_id => v_log_id,
            o_err_msg => v_audit_err
        );

        COMMIT;
        o_err_msg := NULL;

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            o_err_msg := 'Error deleting user: ' || SQLERRM;
    END DELETE_USER;

END USER_MGMT_PKG;
/