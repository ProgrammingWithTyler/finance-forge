-- V001__Create_users_table.sql
-- Description: Creates the USERS table for authentication and user management
-- Author: ProgrammingWithTyler Team
-- Date: 2025-09-29

-- Create sequence for user IDs
CREATE SEQUENCE users_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Create USERS table
CREATE TABLE users (
    id NUMBER(19) DEFAULT users_seq.NEXTVAL NOT NULL,
    username VARCHAR2(50) NOT NULL,
    email VARCHAR2(255) NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    first_name VARCHAR2(100) NOT NULL,
    last_name VARCHAR2(100) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50) NOT NULL,
    updated_by VARCHAR2(50) NOT NULL,
    -- Constraints
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING')),
    CONSTRAINT ck_users_username_length CHECK (LENGTH(username) BETWEEN 3 AND 50),
    CONSTRAINT ck_users_email_format CHECK (REGEXP_LIKE(email, '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')),
    CONSTRAINT ck_users_first_name_length CHECK (LENGTH(first_name) BETWEEN 1 AND 100),
    CONSTRAINT ck_users_last_name_length CHECK (LENGTH(last_name) BETWEEN 1 AND 100)
);

-- Create indexes for common queries
-- Note: username and email already have unique indexes from constraints
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Add table comment
COMMENT ON TABLE users IS 'Stores user account information for authentication and authorization';
COMMENT ON COLUMN users.id IS 'Primary key - auto-generated user identifier';
COMMENT ON COLUMN users.username IS 'Unique username (3-50 characters)';
COMMENT ON COLUMN users.email IS 'Unique email address';
COMMENT ON COLUMN users.password_hash IS 'Hashed password (bcrypt/argon2)';
COMMENT ON COLUMN users.first_name IS 'User first name (1-100 characters)';
COMMENT ON COLUMN users.last_name IS 'User last name (1-100 characters)';
COMMENT ON COLUMN users.status IS 'Account status: PENDING, ACTIVE, or INACTIVE';

-- Create trigger for updated_at timestamp
CREATE OR REPLACE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/