-- V004__create_audit_log_table.sql
-- Author: ProgrammingWithTyler
-- Created: 2025-10-08
-- Purpose: PL/SQL package and table for auditing user/system activity
-- Notes:
--   - All procedures provide human-readable errors via o_err_msg OUT parameter
--   - Designed for integration with Flyway migration system
--   - Tracks inserts into AUDIT_LOG for important actions
--   - Enhanced with indexing, partitioning prep, and additional procedures

-- =======================================================
-- 1. CREATE SEQUENCE FOR AUDIT_LOG
-- =======================================================
CREATE SEQUENCE audit_log_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- =======================================================
-- 2. CREATE AUDIT_LOG TABLE
-- =======================================================
CREATE TABLE audit_log (
    id NUMBER(19) DEFAULT audit_log_seq.NEXTVAL NOT NULL,
    entity_name VARCHAR2(100) NOT NULL,
    entity_id NUMBER(19),
    action_type VARCHAR2(50) NOT NULL,
    action_by VARCHAR2(50) DEFAULT 'SYSTEM' NOT NULL,
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address VARCHAR2(45),  -- IPv6 support
    session_id VARCHAR2(100),
    old_values CLOB,  -- JSON or key-value pairs of old data
    new_values CLOB,  -- JSON or key-value pairs of new data
    description VARCHAR2(4000),
    severity VARCHAR2(20) DEFAULT 'INFO',  -- INFO, WARNING, ERROR, CRITICAL
    PRIMARY KEY (id)
);

COMMENT ON TABLE audit_log IS 'Stores audit entries for system/user actions';
COMMENT ON COLUMN audit_log.entity_name IS 'Table or entity affected';
COMMENT ON COLUMN audit_log.entity_id IS 'ID of affected row';
COMMENT ON COLUMN audit_log.action_type IS 'Type of action: INSERT/UPDATE/DELETE/LOGIN/LOGOUT/etc';
COMMENT ON COLUMN audit_log.action_by IS 'User who performed action';
COMMENT ON COLUMN audit_log.action_timestamp IS 'Timestamp of action';
COMMENT ON COLUMN audit_log.ip_address IS 'IP address of user (supports IPv4 and IPv6)';
COMMENT ON COLUMN audit_log.session_id IS 'Session identifier for tracking user session';
COMMENT ON COLUMN audit_log.old_values IS 'Previous values before change (JSON format)';
COMMENT ON COLUMN audit_log.new_values IS 'New values after change (JSON format)';
COMMENT ON COLUMN audit_log.description IS 'Optional detailed message';
COMMENT ON COLUMN audit_log.severity IS 'Log severity: INFO, WARNING, ERROR, CRITICAL';

-- =======================================================
-- 3. CREATE INDEXES FOR PERFORMANCE
-- =======================================================
CREATE INDEX idx_audit_log_entity ON audit_log(entity_name, entity_id);
CREATE INDEX idx_audit_log_action_by ON audit_log(action_by);
CREATE INDEX idx_audit_log_timestamp ON audit_log(action_timestamp);
CREATE INDEX idx_audit_log_action_type ON audit_log(action_type);
CREATE INDEX idx_audit_log_severity ON audit_log(severity);