# 🛢️ FinanceForge Database Conventions Guide

**Version:** 1.0\
**Last Updated:** 2025-10-08\
**Applies To:** Oracle 21c (Production, Test, and Development Environments)\
**Author:** ProgrammingWithTyler\
**Purpose:** To ensure all SQL and PL/SQL artifacts follow consistent naming, structure, and migration practices for clarity, auditability, and operational stability.

---

## 1. 🎯 Guiding Principles

1. **Predictability Over Cleverness** – The schema should read like a well-written book. No guessing, no decoding.
2. **Explicit Beats Implicit** – Names should say what they mean. Avoid shortcuts and abbreviations unless industry-standard.
3. **Consistency Enables Velocity** – Developers and DBAs should recognize and extend any part of the system without re-learning conventions.
4. **Flyway as Source of Truth** – Every DDL or PL/SQL change must exist as a Flyway migration file under version control.
5. **Auditable by Design** – Naming patterns must survive code reviews, audits, and long-term system evolution.

---

## 2. 🧱 Directory Layout

All migrations are placed under:

```
src/main/resources/db/migration/
```

To maintain clarity and separation of concern, subdirectories are used:

```
db/migration/
├── tables/        → Table creation and alterations
├── sequences/     → Sequence creation
├── packages/      → PL/SQL packages (business logic)
├── triggers/      → DML triggers and audit triggers
├── views/         → Logical or reporting views
└── seed/          → Reference or sample data (optional)
```

Flyway automatically scans all subdirectories, so nesting is fully supported.

---

## 3. 📜 Migration File Naming

Use **semantic, descriptive file names** that indicate both version and purpose.

| Type         | Convention                         | Example                                 |
| ------------ | ---------------------------------- | --------------------------------------- |
| DDL (Tables) | `V###__create_<object>_table.sql`  | `V001__create_users_table.sql`          |
| Sequences    | `V###__create_sequences.sql`       | `V002__create_sequences.sql`            |
| Packages     | `V###__create_<feature>_pkg.sql`   | `V005__create_user_mgmt_pkg.sql`        |
| Triggers     | `V###__create_<table>_trigger.sql` | `V010__create_audit_triggers.sql`       |
| Views        | `V###__create_<feature>_view.sql`  | `V008__create_expense_summary_view.sql` |
| Seed Data    | `V###__seed_<object>.sql`          | `V100__seed_reference_data.sql`         |
| Audit System | `V###__create_audit_log_pkg.sql`   | `V004__create_audit_log_pkg.sql`        |

> 🧩 **Rule of Thumb:**
>
> * Increment `###` sequentially for every schema-affecting change.
> * Use double underscore (`__`) to separate version from description.
> * Use lowercase and underscores throughout for consistency.
> * Group related migrations together (e.g., V001-V003 for core tables, V004-V010 for packages).

---

## 4. 🧩 Naming Conventions

### 4.1 Database Objects

| **Object Type**    | **Convention**              | **Example**                  | **Notes**                                    |
| ------------------ | --------------------------- | ---------------------------- | -------------------------------------------- |
| **Schema**         | `UPPERCASE`                 | `FINANCEFORGE`               | One schema per environment.                  |
| **Tables**         | `UPPERCASE`, plural nouns   | `USERS`, `EXPENSE_CATEGORIES`| Simple, domain-driven.                       |
| **Primary Keys**   | `ID`                        | `ID` column in `USERS`       | Simple and consistent across all tables.     |
| **Foreign Keys**   | `<referenced_table>_id`     | `USER_ID` in `EXPENSES`      | Clarifies join intent.                       |
| **Sequences**      | `<table>_seq`               | `users_seq`, `expenses_seq`  | Lowercase with `_seq` suffix.                |
| **Constraints**    | `pk_`, `fk_`, `ck_`, `uq_`  | `pk_users`, `fk_expenses_users` | Lowercase prefixes for readability.      |
| **Indexes**        | `idx_<table>_<col>`         | `idx_users_email`            | Lowercase for clarity during query tuning.   |
| **Triggers**       | `trg_<table>_<action>`      | `trg_users_audit`            | One trigger per major DML purpose.           |
| **Views**          | `vw_<feature>`              | `vw_expense_summary`         | Lowercase prefix ensures no table collision. |

### 4.2 PL/SQL Objects

| **Object Type**                        | **Convention**                      | **Example**                        | **Notes**                                      |
| -------------------------------------- | ----------------------------------- | ---------------------------------- | ---------------------------------------------- |
| **Packages**                           | `<DOMAIN>_MGMT_PKG`                 | `USER_MGMT_PKG`, `LOG_MGMT_PKG`    | UPPERCASE with `_MGMT_PKG` suffix for management packages. |
| **Specialized Packages**               | `<DOMAIN>_<PURPOSE>_PKG`            | `USER_AUTH_PKG`, `REPORT_GEN_PKG`  | Purpose-specific suffixes: `_AUTH_PKG`, `_GEN_PKG`, etc. |
| **Procedures (in packages)**           | `UPPERCASE`                         | `CREATE_USER`, `GET_USER_BY_ID`    | Clear, action-oriented verbs.                  |
| **Functions (in packages)**            | `UPPERCASE`                         | `CALCULATE_TOTAL`, `IS_VALID_EMAIL`| Return value should be obvious from name.      |
| **Package Variables**                  | `v_<name>` (local), `g_<name>` (global) | `v_user_id`, `g_max_attempts`  | Prefix distinguishes scope.                    |
| **Cursor Parameters**                  | `p_cursor`                          | `p_cursor OUT SYS_REFCURSOR`       | Consistent across all query procedures.        |
| **Input Parameters**                   | `p_<name>`                          | `p_user_id`, `p_email`             | `p_` prefix for all IN parameters.             |
| **Output Parameters**                  | `p_<name>` (data), `o_err_msg` (errors) | `p_user_id OUT NUMBER`, `o_err_msg OUT VARCHAR2` | `o_err_msg` reserved for error messages. |

### 4.3 Package Naming Standards

FinanceForge uses a **domain-driven package naming strategy** with consistent suffixes:

| **Suffix**        | **Purpose**                     | **Example**                  |
| ----------------- | ------------------------------- | ---------------------------- |
| `_MGMT_PKG`       | Entity CRUD operations          | `USER_MGMT_PKG`, `ACCOUNT_MGMT_PKG` |
| `_AUTH_PKG`       | Authentication logic            | `USER_AUTH_PKG`              |
| `_SECURITY_PKG`   | Authorization/permissions       | `USER_SECURITY_PKG`          |
| `_GEN_PKG`        | Generation utilities            | `REPORT_GEN_PKG`             |
| `_UTIL_PKG`       | Utility functions               | `STRING_UTIL_PKG`            |
| `_PKG`            | General purpose                 | `VALIDATION_PKG`, `NOTIFICATION_PKG` |

**Rationale:** This pattern scales as the system grows and allows for clear separation of concerns. For example:
- `USER_MGMT_PKG` handles user CRUD operations
- `USER_AUTH_PKG` handles login/logout/password management
- `USER_SECURITY_PKG` handles roles and permissions

---

## 5. ⚙️ Error Handling & Logging (PL/SQL)

### 5.1 Standard Error Handling Pattern

Every public procedure/function must include:
1. An `o_err_msg OUT VARCHAR2` parameter for error messages
2. Explicit exception handling for known errors
3. Integration with `LOG_MGMT_PKG` for critical operations

### 5.2 Error Message Guidelines

- **User-facing errors**: Clear, actionable messages
    - ✅ `'Username "john123" is already taken'`
    - ❌ `'ORA-00001: unique constraint violated'`

- **System errors**: Include context but sanitize sensitive data
    - ✅ `'Error creating user: Invalid data format'`
    - ❌ `'Error: password123 failed validation'`

### 5.3 Exception Handling Template

```sql
EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        o_err_msg := 'Duplicate username or email detected.';
        ROLLBACK;
    WHEN NO_DATA_FOUND THEN
        o_err_msg := 'No record found with specified criteria.';
    WHEN OTHERS THEN
        o_err_msg := 'System error: ' || SQLERRM;
        ROLLBACK;
END;
```

### 5.4 Audit Logging Integration

All mutation operations (INSERT, UPDATE, DELETE) should log to `audit_log` via `LOG_MGMT_PKG`:

```sql
-- After successful operation
LOG_MGMT_PKG.CREATE_LOG_ENTRY(
    p_entity_name => 'USERS',
    p_entity_id => v_user_id,
    p_action_type => 'INSERT',
    p_action_by => p_created_by,
    p_description => 'Created user: ' || p_username,
    p_severity => 'INFO',
    p_log_id => v_log_id,
    o_err_msg => v_audit_err
);
```

---

## 6. 🔒 Transaction Management

### 6.1 COMMIT/ROLLBACK Strategy

- **Explicit commits**: All mutation procedures must explicitly COMMIT or ROLLBACK
- **Autonomous transactions**: Use `PRAGMA AUTONOMOUS_TRANSACTION` only for logging
- **Java transactions**: Spring's `@Transactional` coordinates with PL/SQL commits

Example:
```sql
PROCEDURE CREATE_USER(...) IS
BEGIN
    -- Validation
    IF validation_fails THEN
        o_err_msg := 'Validation error';
        RETURN;  -- No ROLLBACK needed, nothing changed
    END IF;
    
    -- Insert operation
    INSERT INTO USERS (...) VALUES (...);
    
    -- Audit logging
    LOG_MGMT_PKG.CREATE_LOG_ENTRY(...);
    
    COMMIT;  -- Explicit commit
    o_err_msg := NULL;
    
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;  -- Explicit rollback
        o_err_msg := 'Error: ' || SQLERRM;
END;
```

---

## 7. 🧪 Testing & Repeatability

* **TestContainers** will spin up ephemeral Oracle instances during CI to run migrations automatically.
* Flyway must succeed on every container launch; no manual DDL drift allowed.
* Packages and views are versioned the same way as tables – all migrations remain **immutable** once merged.
* Integration tests should verify:
    - Package procedures execute successfully
    - Error messages are returned correctly via `o_err_msg`
    - Audit logs are created for all mutations
    - Transaction boundaries work correctly

---

## 8. 🪶 Best Practices

### General

✅ Follow naming conventions rigorously – they enable pattern recognition\
✅ Document all public procedures/functions with clear parameter descriptions\
✅ Keep all DDL and PL/SQL logic under version control – no ad-hoc SQL in production\
✅ Never modify or reorder old migration files. Create a new one\
✅ Use meaningful, descriptive names over brevity

### PL/SQL Specific

✅ Always include `o_err_msg OUT VARCHAR2` for error handling\
✅ Use explicit COMMIT/ROLLBACK in mutation procedures\
✅ Integrate with `LOG_MGMT_PKG` for all critical operations\
✅ Validate input parameters before database operations\
✅ Use helper functions for reusable logic (e.g., `username_exists()`)\
✅ Keep package specs and bodies in sync

### Java Integration

✅ Check `o_err_msg` after every stored procedure call\
✅ Map PL/SQL errors to appropriate Spring exceptions\
✅ Use `@Transactional` for coordinated transaction management\
✅ Log at appropriate levels (DEBUG for queries, INFO for mutations, ERROR for failures)

---

## 9. 🧭 Example File Tree

```
src/main/resources/db/migration/
├── tables/
│   ├── V001__create_users_table.sql
│   ├── V002__create_expense_categories_table.sql
│   └── V003__create_expenses_table.sql
├── sequences/
│   └── V002__create_sequences.sql (if not included in table migrations)
├── packages/
│   ├── V004__create_audit_log_pkg.sql         (LOG_MGMT_PKG)
│   ├── V005__create_user_mgmt_pkg.sql         (USER_MGMT_PKG)
│   ├── V006__create_account_mgmt_pkg.sql      (ACCOUNT_MGMT_PKG)
│   └── V007__create_user_auth_pkg.sql         (USER_AUTH_PKG)
├── views/
│   └── V020__create_expense_summary_view.sql
├── triggers/
│   └── V030__create_audit_triggers.sql
└── seed/
    └── V100__seed_reference_data.sql
```

---

## 10. 📋 Migration Checklist

Before submitting a migration for review:

- [ ] File name follows `V###__create_<feature>.sql` convention
- [ ] All table names are UPPERCASE
- [ ] All sequences use lowercase with `_seq` suffix
- [ ] Package names use `<DOMAIN>_MGMT_PKG` or similar pattern
- [ ] All procedures include `o_err_msg OUT VARCHAR2`
- [ ] Mutation operations include explicit COMMIT/ROLLBACK
- [ ] Critical operations integrate with `LOG_MGMT_PKG`
- [ ] Cursor parameters consistently named `p_cursor`
- [ ] Input parameters prefixed with `p_`
- [ ] File includes header comment with author, date, purpose
- [ ] Migration is idempotent (can run multiple times safely)
- [ ] Code reviewed for SQL injection vulnerabilities
- [ ] Tested locally with Flyway before committing

---

## 11. 🏗️ Future Enhancements

### Planned Packages (Roadmap)

**Phase 1: Core User Management** (Current)
- ✅ `LOG_MGMT_PKG` - Audit logging
- ✅ `USER_MGMT_PKG` - User CRUD operations

**Phase 2: Authentication & Security**
- `USER_AUTH_PKG` - Login, logout, password reset
- `USER_SESSION_PKG` - Session tracking
- `USER_SECURITY_PKG` - Roles and permissions

**Phase 3: Financial Core**
- `ACCOUNT_MGMT_PKG` - Bank accounts
- `TRANSACTION_MGMT_PKG` - Financial transactions
- `BUDGET_MGMT_PKG` - Budget planning
- `CATEGORY_MGMT_PKG` - Transaction categories

**Phase 4: Reporting & Analytics**
- `REPORT_GEN_PKG` - Report generation
- `ANALYTICS_PKG` - Data analytics
- `DASHBOARD_PKG` - Dashboard queries

---

## 12. 📚 References

### Oracle Documentation
- [PL/SQL Packages and Types](https://docs.oracle.com/en/database/oracle/oracle-database/21/lnpls/plsql-packages.html)
- [Exception Handling](https://docs.oracle.com/en/database/oracle/oracle-database/21/lnpls/plsql-error-handling.html)

### FinanceForge Internal
- See `UserRepositoryImpl.java` for Java-PL/SQL integration patterns
- See `V004__create_audit_log_pkg.sql` for audit logging implementation
- See `V005__create_user_mgmt_pkg.sql` for management package template

---

## 13. 🎓 Closing Statement

> "Database discipline is not bureaucracy – it's craftsmanship."\
> – FinanceForge Engineering Standards

All contributors are expected to follow this document rigorously. If a migration violates the naming or structure conventions, it will be rejected during code review.

**Questions or clarifications?** Contact ProgrammingWithTyler or open a discussion in the team channel.

---

**Document History:**
- v1.0 (Initial): Established core conventions