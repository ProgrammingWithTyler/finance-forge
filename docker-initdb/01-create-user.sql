-- 01-create-user.sql
-- This script runs when Oracle container first starts

-- Connect to the pluggable database
ALTER SESSION SET CONTAINER = XEPDB1;

-- Create the financeforge user
CREATE USER financeforge IDENTIFIED BY "FF_Secure_2024!";

-- Grant necessary privileges
GRANT CONNECT, RESOURCE TO financeforge;
GRANT CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE TO financeforge;
GRANT UNLIMITED TABLESPACE TO financeforge;

-- Set default tablespace
ALTER USER financeforge DEFAULT TABLESPACE USERS;
ALTER USER financeforge TEMPORARY TABLESPACE TEMP;

EXIT;

-- Run only if the user doesn't exist
-- BEGIN
--    EXECUTE IMMEDIATE 'CREATE USER financeforge IDENTIFIED BY "FF_Secure_2024!"';
-- EXCEPTION
--    WHEN OTHERS THEN
--       IF SQLCODE != -01920 THEN
--          RAISE;
--       END IF;
-- END;
-- /

-- -- Grant proper privileges
-- BEGIN
--    EXECUTE IMMEDIATE 'GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE TRIGGER TO financeforge';
-- END;
-- /


-- -- 01-create-user.sql  
-- -- This script runs when Oracle container first starts

-- -- Switch to the pluggable database
-- ALTER SESSION SET CONTAINER = XEPDB1;

-- -- Drop user if it exists (for clean rebuilds)
-- BEGIN
--   EXECUTE IMMEDIATE 'DROP USER financeforge CASCADE';
-- EXCEPTION
--   WHEN OTHERS THEN NULL;
-- END;
-- /

-- -- Create the financeforge user with proper settings
-- CREATE USER financeforge IDENTIFIED BY "FF_Secure_2024!";

-- -- Set tablespaces after user creation
-- ALTER USER financeforge DEFAULT TABLESPACE USERS;
-- ALTER USER financeforge TEMPORARY TABLESPACE TEMP;

-- -- Grant necessary privileges
-- GRANT CONNECT TO financeforge;
-- GRANT RESOURCE TO financeforge;
-- GRANT CREATE TABLE TO financeforge;
-- GRANT CREATE SEQUENCE TO financeforge;
-- GRANT CREATE PROCEDURE TO financeforge;
-- GRANT CREATE VIEW TO financeforge;
-- GRANT UNLIMITED TABLESPACE TO financeforge;

-- -- Verify user was created successfully
-- SELECT username, default_tablespace, temporary_tablespace, account_status
-- FROM dba_users 
-- WHERE username = 'FINANCEFORGE';

-- EXIT;