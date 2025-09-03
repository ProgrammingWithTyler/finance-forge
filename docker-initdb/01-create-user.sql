-- Connect to the pluggable database
ALTER SESSION SET CONTAINER = XEPDB1;

-- Drop user if exists (to start clean)
BEGIN
   EXECUTE IMMEDIATE 'DROP USER financeforge CASCADE';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

-- Create the financeforge user
CREATE USER financeforge IDENTIFIED BY "FinanceForge2025!";

-- Basic connection privileges
GRANT CONNECT TO financeforge;
GRANT RESOURCE TO financeforge;
GRANT CREATE SESSION TO financeforge;

-- Object creation privileges (what Flyway needs)
GRANT CREATE TABLE TO financeforge;
GRANT CREATE SEQUENCE TO financeforge;
GRANT CREATE VIEW TO financeforge;
GRANT CREATE PROCEDURE TO financeforge;
GRANT CREATE TRIGGER TO financeforge;
GRANT CREATE SYNONYM TO financeforge;

-- Index privileges (through RESOURCE role, but let's be explicit)
GRANT CREATE ANY INDEX TO financeforge;
GRANT DROP ANY INDEX TO financeforge;

-- Schema creation privilege (this is what's missing!)
GRANT CREATE ANY SCHEMA TO financeforge;

-- Additional privileges Flyway might need
GRANT ALTER ANY TABLE TO financeforge;
GRANT DROP ANY TABLE TO financeforge;
GRANT SELECT ANY TABLE TO financeforge;
GRANT INSERT ANY TABLE TO financeforge;
GRANT UPDATE ANY TABLE TO financeforge;
GRANT DELETE ANY TABLE TO financeforge;

-- Grant access to system views (for Flyway metadata queries)
GRANT SELECT ON dba_objects TO financeforge;
GRANT SELECT ON dba_tables TO financeforge;
GRANT SELECT ON dba_constraints TO financeforge;
GRANT SELECT ON dba_indexes TO financeforge;

-- Tablespace quota
ALTER USER financeforge QUOTA UNLIMITED ON USERS;

-- Set default schema
ALTER USER financeforge DEFAULT TABLESPACE USERS;
ALTER USER financeforge TEMPORARY TABLESPACE TEMP;

-- Verify creation
SELECT 'User financeforge created with full privileges' AS status FROM dual;

COMMIT;