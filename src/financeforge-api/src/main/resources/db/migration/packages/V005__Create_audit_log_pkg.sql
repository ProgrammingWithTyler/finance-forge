-- V006__create_audit_log_pkg.sql
-- Author: ProgrammingWithTyler
-- Created: 2025-10-08
-- Purpose: PL/SQL package and table for auditing user/system activity
-- Notes:
--   - All procedures provide human-readable errors via o_err_msg OUT parameter
--   - Designed for integration with Flyway migration system
--   - Tracks inserts into AUDIT_LOG for important actions
--   - Enhanced with indexing, partitioning prep, and additional procedures

-- =======================================================
-- 4. PACKAGE SPEC: LOG_MGMT_PKG
-- =======================================================
CREATE OR REPLACE PACKAGE LOG_MGMT_PKG AS

    -- Main audit logging procedure
    PROCEDURE CREATE_LOG_ENTRY(
        p_entity_name   IN  VARCHAR2,
        p_entity_id     IN  NUMBER,
        p_action_type   IN  VARCHAR2,
        p_action_by     IN  VARCHAR2 DEFAULT 'SYSTEM',
        p_description   IN  VARCHAR2 DEFAULT NULL,
        p_ip_address    IN  VARCHAR2 DEFAULT NULL,
        p_session_id    IN  VARCHAR2 DEFAULT NULL,
        p_old_values    IN  CLOB DEFAULT NULL,
        p_new_values    IN  CLOB DEFAULT NULL,
        p_severity      IN  VARCHAR2 DEFAULT 'INFO',
        p_log_id        OUT NUMBER,
        o_err_msg       OUT VARCHAR2
    );

    -- Simplified logging for basic actions
    PROCEDURE LOG_ACTION(
        p_entity_name   IN  VARCHAR2,
        p_entity_id     IN  NUMBER,
        p_action_type   IN  VARCHAR2,
        p_action_by     IN  VARCHAR2 DEFAULT 'SYSTEM',
        p_description   IN  VARCHAR2 DEFAULT NULL
    );

    -- Query audit logs with filters
    PROCEDURE GET_AUDIT_LOGS(
        p_entity_name   IN  VARCHAR2 DEFAULT NULL,
        p_entity_id     IN  NUMBER DEFAULT NULL,
        p_action_by     IN  VARCHAR2 DEFAULT NULL,
        p_start_date    IN  TIMESTAMP DEFAULT NULL,
        p_end_date      IN  TIMESTAMP DEFAULT NULL,
        p_cursor        OUT SYS_REFCURSOR,
        o_err_msg       OUT VARCHAR2
    );

    -- Purge old audit logs (for maintenance)
    PROCEDURE PURGE_OLD_LOGS(
        p_days_to_keep  IN  NUMBER DEFAULT 365,
        p_rows_deleted  OUT NUMBER,
        o_err_msg       OUT VARCHAR2
    );

    -- Get audit trail for specific entity
    PROCEDURE GET_ENTITY_HISTORY(
        p_entity_name   IN  VARCHAR2,
        p_entity_id     IN  NUMBER,
        p_cursor        OUT SYS_REFCURSOR,
        o_err_msg       OUT VARCHAR2
    );

END LOG_MGMT_PKG;
/

-- =======================================================
-- 5. PACKAGE BODY: LOG_MGMT_PKG
-- =======================================================
CREATE OR REPLACE PACKAGE BODY LOG_MGMT_PKG AS

    -- Main audit logging procedure with all options
    PROCEDURE CREATE_LOG_ENTRY(
        p_entity_name   IN  VARCHAR2,
        p_entity_id     IN  NUMBER,
        p_action_type   IN  VARCHAR2,
        p_action_by     IN  VARCHAR2 DEFAULT 'SYSTEM',
        p_description   IN  VARCHAR2 DEFAULT NULL,
        p_ip_address    IN  VARCHAR2 DEFAULT NULL,
        p_session_id    IN  VARCHAR2 DEFAULT NULL,
        p_old_values    IN  CLOB DEFAULT NULL,
        p_new_values    IN  CLOB DEFAULT NULL,
        p_severity      IN  VARCHAR2 DEFAULT 'INFO',
        p_log_id        OUT NUMBER,
        o_err_msg       OUT VARCHAR2
    ) IS
        v_severity VARCHAR2(20);
    BEGIN
        -- Validate severity
        v_severity := UPPER(NVL(p_severity, 'INFO'));
        IF v_severity NOT IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL') THEN
            v_severity := 'INFO';
        END IF;

        -- Validate action_type is not empty
        IF p_action_type IS NULL OR TRIM(p_action_type) IS NULL THEN
            o_err_msg := 'Action type cannot be null or empty';
            RETURN;
        END IF;

        -- Validate entity_name is not empty
        IF p_entity_name IS NULL OR TRIM(p_entity_name) IS NULL THEN
            o_err_msg := 'Entity name cannot be null or empty';
            RETURN;
        END IF;

        INSERT INTO audit_log (
            entity_name,
            entity_id,
            action_type,
            action_by,
            action_timestamp,
            ip_address,
            session_id,
            old_values,
            new_values,
            description,
            severity
        ) VALUES (
            UPPER(TRIM(p_entity_name)),
            p_entity_id,
            UPPER(TRIM(p_action_type)),
            p_action_by,
            CURRENT_TIMESTAMP,
            p_ip_address,
            p_session_id,
            p_old_values,
            p_new_values,
            p_description,
            v_severity
        )
        RETURNING id INTO p_log_id;

        COMMIT;
        o_err_msg := NULL;

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            o_err_msg := 'Error creating audit log: ' || SQLERRM;
            p_log_id := NULL;
    END CREATE_LOG_ENTRY;

    -- Simplified logging procedure (autonomous transaction)
    PROCEDURE LOG_ACTION(
        p_entity_name   IN  VARCHAR2,
        p_entity_id     IN  NUMBER,
        p_action_type   IN  VARCHAR2,
        p_action_by     IN  VARCHAR2 DEFAULT 'SYSTEM',
        p_description   IN  VARCHAR2 DEFAULT NULL
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
        v_log_id NUMBER;
        v_err_msg VARCHAR2(4000);
    BEGIN
        CREATE_LOG_ENTRY(
            p_entity_name => p_entity_name,
            p_entity_id => p_entity_id,
            p_action_type => p_action_type,
            p_action_by => p_action_by,
            p_description => p_description,
            p_log_id => v_log_id,
            o_err_msg => v_err_msg
        );

        IF v_err_msg IS NOT NULL THEN
            DBMS_OUTPUT.PUT_LINE('Audit log warning: ' || v_err_msg);
        END IF;
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            -- Silent failure to not disrupt main transaction
            DBMS_OUTPUT.PUT_LINE('Audit log failed: ' || SQLERRM);
    END LOG_ACTION;

    -- Query audit logs with filters
    PROCEDURE GET_AUDIT_LOGS(
        p_entity_name   IN  VARCHAR2 DEFAULT NULL,
        p_entity_id     IN  NUMBER DEFAULT NULL,
        p_action_by     IN  VARCHAR2 DEFAULT NULL,
        p_start_date    IN  TIMESTAMP DEFAULT NULL,
        p_end_date      IN  TIMESTAMP DEFAULT NULL,
        p_cursor        OUT SYS_REFCURSOR,
        o_err_msg       OUT VARCHAR2
    ) IS
    BEGIN
        OPEN p_cursor FOR
            SELECT
                id,
                entity_name,
                entity_id,
                action_type,
                action_by,
                action_timestamp,
                ip_address,
                session_id,
                old_values,
                new_values,
                description,
                severity
            FROM audit_log
            WHERE (p_entity_name IS NULL OR entity_name = UPPER(p_entity_name))
              AND (p_entity_id IS NULL OR entity_id = p_entity_id)
              AND (p_action_by IS NULL OR action_by = p_action_by)
              AND (p_start_date IS NULL OR action_timestamp >= p_start_date)
              AND (p_end_date IS NULL OR action_timestamp <= p_end_date)
            ORDER BY action_timestamp DESC;

        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving audit logs: ' || SQLERRM;
    END GET_AUDIT_LOGS;

    -- Purge old audit logs
    PROCEDURE PURGE_OLD_LOGS(
        p_days_to_keep  IN  NUMBER DEFAULT 365,
        p_rows_deleted  OUT NUMBER,
        o_err_msg       OUT VARCHAR2
    ) IS
        v_cutoff_date TIMESTAMP;
    BEGIN
        IF p_days_to_keep < 30 THEN
            o_err_msg := 'Cannot purge logs newer than 30 days';
            RETURN;
        END IF;

        v_cutoff_date := CURRENT_TIMESTAMP - NUMTODSINTERVAL(p_days_to_keep, 'DAY');

        DELETE FROM audit_log
        WHERE action_timestamp < v_cutoff_date;

        p_rows_deleted := SQL%ROWCOUNT;
        COMMIT;
        o_err_msg := NULL;

    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            o_err_msg := 'Error purging audit logs: ' || SQLERRM;
            p_rows_deleted := 0;
    END PURGE_OLD_LOGS;

    -- Get complete audit trail for specific entity
    PROCEDURE GET_ENTITY_HISTORY(
        p_entity_name   IN  VARCHAR2,
        p_entity_id     IN  NUMBER,
        p_cursor        OUT SYS_REFCURSOR,
        o_err_msg       OUT VARCHAR2
    ) IS
    BEGIN
        IF p_entity_name IS NULL OR p_entity_id IS NULL THEN
            o_err_msg := 'Entity name and ID are required';
            RETURN;
        END IF;

        OPEN p_cursor FOR
            SELECT
                id,
                action_type,
                action_by,
                action_timestamp,
                ip_address,
                old_values,
                new_values,
                description,
                severity
            FROM audit_log
            WHERE entity_name = UPPER(p_entity_name)
              AND entity_id = p_entity_id
            ORDER BY action_timestamp ASC;

        o_err_msg := NULL;
    EXCEPTION
        WHEN OTHERS THEN
            o_err_msg := 'Error retrieving entity history: ' || SQLERRM;
    END GET_ENTITY_HISTORY;

END LOG_MGMT_PKG;
/