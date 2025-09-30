-- V003__Create_expenses_table.sql
-- Description: Creates the EXPENSES table for tracking financial transactions
-- Author: ProgrammingWithTyler Team
-- Date: 2025-09-29

-- Create sequence for expense IDs
CREATE SEQUENCE expenses_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Create EXPENSES table
CREATE TABLE expenses (
    id NUMBER(19) DEFAULT expenses_seq.NEXTVAL NOT NULL,
    user_id NUMBER(19) NOT NULL,
    category_id NUMBER(19) NOT NULL,
    amount NUMBER(15,2) NOT NULL,
    currency_code VARCHAR2(3) DEFAULT 'USD' NOT NULL,
    description VARCHAR2(500) NOT NULL,
    expense_date DATE NOT NULL,
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50) DEFAULT 'SYSTEM' NOT NULL,
    updated_by VARCHAR2(50) DEFAULT 'SYSTEM' NOT NULL,
    -- Constraints
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id)
        REFERENCES expense_categories(id),
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_expenses_amount_max CHECK (amount < 1000000),
    CONSTRAINT ck_expenses_description_length CHECK (LENGTH(description) BETWEEN 1 AND 500),
    CONSTRAINT ck_expenses_currency_code CHECK (LENGTH(currency_code) = 3)
);

-- Create indexes for common queries
CREATE INDEX idx_expenses_user_id ON expenses(user_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
CREATE INDEX idx_expenses_created_at ON expenses(created_at);
CREATE INDEX idx_expenses_amount ON expenses(amount);

-- Create composite index for common user+date queries
CREATE INDEX idx_expenses_user_date ON expenses(user_id, expense_date DESC);

-- Add table comments
COMMENT ON TABLE expenses IS 'Stores individual expense transactions for users';
COMMENT ON COLUMN expenses.id IS 'Primary key - auto-generated expense identifier';
COMMENT ON COLUMN expenses.user_id IS 'Foreign key to users table - expense owner';
COMMENT ON COLUMN expenses.category_id IS 'Foreign key to expense_categories table';
COMMENT ON COLUMN expenses.amount IS 'Expense amount in currency (positive, max $999,999.99)';
COMMENT ON COLUMN expenses.currency_code IS 'ISO 4217 currency code (e.g., USD, EUR, GBP)';
COMMENT ON COLUMN expenses.description IS 'Required description of expense (1-500 characters)';
COMMENT ON COLUMN expenses.expense_date IS 'Date of expense (not more than 1 year in future)';

-- Create trigger for updated_at timestamp
CREATE OR REPLACE TRIGGER trg_expenses_updated_at
    BEFORE UPDATE ON expenses
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Create trigger to validate category ownership
CREATE OR REPLACE TRIGGER trg_expenses_validate_category
    BEFORE INSERT OR UPDATE ON expenses
    FOR EACH ROW
DECLARE
    v_category_user_id NUMBER(19);
BEGIN
    -- Ensure the category belongs to the same user as the expense
    SELECT user_id INTO v_category_user_id
    FROM expense_categories
    WHERE id = :NEW.category_id;

    IF v_category_user_id != :NEW.user_id THEN
        RAISE_APPLICATION_ERROR(-20001, 'Category does not belong to user');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, 'Category not found');
END;
/