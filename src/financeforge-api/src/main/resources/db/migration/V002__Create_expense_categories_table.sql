-- V002__Create_expense_categories_table.sql
-- Description: Creates the EXPENSE_CATEGORIES table for organizing expenses
-- Author: ProgrammingWithTyler Team
-- Date: 2025-09-29

-- Create sequence for category IDs
CREATE SEQUENCE expense_categories_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Create EXPENSE_CATEGORIES table
CREATE TABLE expense_categories (
    id NUMBER(19) DEFAULT expense_categories_seq.NEXTVAL NOT NULL,
    user_id NUMBER(19) NOT NULL,
    name VARCHAR2(100) NOT NULL,
    description VARCHAR2(500),
    is_system CHAR(1) DEFAULT 'N' NOT NULL,
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50) DEFAULT 'SYSTEM' NOT NULL,
    updated_by VARCHAR2(50) DEFAULT 'SYSTEM' NOT NULL,
    -- Constraints
    CONSTRAINT pk_expense_categories PRIMARY KEY (id),
    CONSTRAINT fk_expense_categories_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_expense_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT ck_expense_categories_name_length CHECK (LENGTH(name) BETWEEN 1 AND 100),
    CONSTRAINT ck_expense_categories_is_system CHECK (is_system IN ('Y', 'N'))
);

-- Create indexes for common queries
CREATE INDEX idx_expense_categories_user_id ON expense_categories(user_id);
CREATE INDEX idx_expense_categories_name ON expense_categories(name);
CREATE INDEX idx_expense_categories_created_at ON expense_categories(created_at);

-- Add table comments
COMMENT ON TABLE expense_categories IS 'User-specific expense categories (flat structure for MVP)';
COMMENT ON COLUMN expense_categories.id IS 'Primary key - auto-generated category identifier';
COMMENT ON COLUMN expense_categories.user_id IS 'Foreign key to users table - category owner';
COMMENT ON COLUMN expense_categories.name IS 'Category name (unique per user)';
COMMENT ON COLUMN expense_categories.description IS 'Optional category description';
COMMENT ON COLUMN expense_categories.is_system IS 'System category flag (Y/N) - system categories cannot be deleted';

-- Create trigger for updated_at timestamp
CREATE OR REPLACE TRIGGER trg_expense_categories_updated_at
    BEFORE UPDATE ON expense_categories
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Insert default categories for testing (optional - can be removed for production)
-- Note: These would typically be created when a user registers
COMMENT ON TABLE expense_categories IS 'User-specific expense categories - default categories created at user registration';