package com.financeforge.api.repository.impl;

import com.financeforge.api.domain.model.User;
import com.financeforge.api.domain.model.enums.UserStatus;
import com.financeforge.api.repository.UserRepository;
import com.financeforge.api.repository.mapper.UserRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Oracle PL/SQL implementation of the UserRepository interface.
 * <p>
 * This repository delegates all CRUD operations to Oracle stored procedures in the USER_MGMT_PKG package.
 * All database interactions are performed through {@link SimpleJdbcCall} to invoke PL/SQL procedures.
 * <p>
 * The implementation provides:
 * <ul>
 *   <li>Full CRUD operations for User entities</li>
 *   <li>Query operations by username, email, and status</li>
 *   <li>Automatic error handling from PL/SQL o_err_msg output parameters</li>
 *   <li>Comprehensive logging for debugging and audit purposes</li>
 *   <li>Transaction management via Spring's @Transactional</li>
 * </ul>
 * <p>
 * <b>Error Handling:</b> All stored procedures return an o_err_msg OUT parameter. This implementation
 * checks for errors after each call and maps them to appropriate Spring exceptions:
 * <ul>
 *   <li>{@link DataIntegrityViolationException} - for duplicate username/email violations</li>
 *   <li>{@link DataAccessException} - for all other database errors</li>
 * </ul>
 * <p>
 * <b>Database Schema:</b> Requires Oracle USER_MGMT_PKG package with the following procedures:
 * <ul>
 *   <li>CREATE_USER - Inserts new user with audit logging</li>
 *   <li>GET_USER_BY_ID - Retrieves user by primary key</li>
 *   <li>GET_BY_EMAIL - Retrieves user by email address</li>
 *   <li>FIND_BY_USERNAME - Retrieves user by username</li>
 *   <li>FIND_ALL_ACTIVE_USERS - Retrieves all users with ACTIVE status</li>
 *   <li>FIND_BY_STATUS - Retrieves users by any status (ACTIVE, INACTIVE, PENDING)</li>
 *   <li>UPDATE_USER - Updates user details with audit logging</li>
 * </ul>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 * @see UserRepository
 * @see User
 * @see UserStatus
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall spGetUserById;
    private final SimpleJdbcCall spCreateUser;
    private final SimpleJdbcCall spUpdateUser;
    private final SimpleJdbcCall spFindByEmail;
    private final SimpleJdbcCall spFindByUsername;
    private final SimpleJdbcCall spFindAllActiveUsers;
    private final SimpleJdbcCall spFindByStatus;

    /**
     * Constructs a new UserRepositoryImpl and initializes all stored procedure calls.
     * <p>
     * This constructor creates {@link SimpleJdbcCall} instances for each stored procedure
     * in the USER_MGMT_PKG package. Query procedures are configured with a {@link UserRowMapper}
     * to map result sets to User objects. Mutation procedures (CREATE, UPDATE) are configured
     * with output parameters for error handling.
     *
     * @param jdbcTemplate the Spring JdbcTemplate for database access, must not be null
     */
    public UserRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        // Initialize all stored procedure calls with USER_MGMT_PKG
        this.spGetUserById = buildCall("GET_USER_BY_ID", new UserRowMapper());
        this.spFindByEmail = buildCall("GET_BY_EMAIL", new UserRowMapper());
        this.spFindByUsername = buildCall("FIND_BY_USERNAME", new UserRowMapper());
        this.spFindAllActiveUsers = buildCall("FIND_ALL_ACTIVE_USERS", new UserRowMapper());
        this.spFindByStatus = buildCall("FIND_BY_STATUS", new UserRowMapper());

        this.spCreateUser = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("USER_MGMT_PKG")
                .withProcedureName("CREATE_USER")
                .declareParameters(
                        new SqlOutParameter("p_user_id", Types.NUMERIC),
                        new SqlOutParameter("o_err_msg", Types.VARCHAR)
                );

        this.spUpdateUser = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("USER_MGMT_PKG")
                .withProcedureName("UPDATE_USER")
                .declareParameters(
                        new SqlOutParameter("o_err_msg", Types.VARCHAR)
                );
    }

    /**
     * Retrieves a user by their unique identifier.
     * <p>
     * Calls the USER_MGMT_PKG.GET_USER_BY_ID stored procedure.
     *
     * @param id the user ID to search for, must not be null
     * @return an Optional containing the User if found, or empty if not found
     * @throws DataAccessException if a database error occurs during retrieval
     * @see User#getId()
     */
    @Override
    public Optional<User> findById(Long id) {
        log.debug("Finding user by id={}", id);

        var params = new MapSqlParameterSource()
                .addValue("p_user_id", id);

        Map<String, Object> result = spGetUserById.execute(params);

        // Check for errors from stored procedure
        checkForErrors(result, "GET_USER_BY_ID");

        List<User> users = extractUsers(result);
        log.debug("Found {} user(s) with id={}", users.size(), id);

        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /**
     * Retrieves a user by their unique username.
     * <p>
     * Calls the USER_MGMT_PKG.FIND_BY_USERNAME stored procedure.
     * Username matching is case-sensitive.
     *
     * @param username the username to search for, must not be null
     * @return an Optional containing the User if found, or empty if not found
     * @throws DataAccessException if a database error occurs during retrieval
     * @see User#getUsername()
     */
    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username='{}'", username);

        var params = new MapSqlParameterSource()
                .addValue("p_username", username);

        Map<String, Object> result = spFindByUsername.execute(params);
        checkForErrors(result, "FIND_BY_USERNAME");

        List<User> users = extractUsers(result);
        log.debug("Found {} user(s) with username='{}'", users.size(), username);

        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /**
     * Retrieves a user by their email address.
     * <p>
     * Calls the USER_MGMT_PKG.GET_BY_EMAIL stored procedure.
     * Email matching is case-sensitive.
     *
     * @param email the email address to search for, must not be null
     * @return an Optional containing the User if found, or empty if not found
     * @throws DataAccessException if a database error occurs during retrieval
     * @see User#getEmail()
     */
    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email='{}'", email);

        var params = new MapSqlParameterSource()
                .addValue("p_email", email);

        Map<String, Object> result = spFindByEmail.execute(params);
        checkForErrors(result, "GET_BY_EMAIL");

        List<User> users = extractUsers(result);
        log.debug("Found {} user(s) with email='{}'", users.size(), email);

        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /**
     * Retrieves all users with ACTIVE status.
     * <p>
     * Calls the USER_MGMT_PKG.FIND_ALL_ACTIVE_USERS stored procedure.
     * Results are ordered by username alphabetically.
     *
     * @return a list of all active users, empty list if none found
     * @throws DataAccessException if a database error occurs during retrieval
     * @see UserStatus#ACTIVE
     */
    @Override
    public List<User> findAllActiveUsers() {
        log.debug("Finding all active users");

        Map<String, Object> result = spFindAllActiveUsers.execute();
        checkForErrors(result, "FIND_ALL_ACTIVE_USERS");

        List<User> users = extractUsers(result);
        log.debug("Found {} active user(s)", users.size());

        return users;
    }

    /**
     * Retrieves all users with the specified status.
     * <p>
     * Calls the USER_MGMT_PKG.FIND_BY_STATUS stored procedure.
     * Results are ordered by username alphabetically.
     *
     * @param userStatus the status to filter by (ACTIVE, INACTIVE, or PENDING), must not be null
     * @return a list of users with the specified status, empty list if none found
     * @throws DataAccessException if a database error occurs or if an invalid status is provided
     * @see UserStatus
     */
    @Override
    public List<User> findByStatus(UserStatus userStatus) {
        log.debug("Finding users by status={}", userStatus);

        var params = new MapSqlParameterSource()
                .addValue("p_status", userStatus.name());

        Map<String, Object> result = spFindByStatus.execute(params);
        checkForErrors(result, "FIND_BY_STATUS");

        List<User> users = extractUsers(result);
        log.debug("Found {} user(s) with status={}", users.size(), userStatus);

        return users;
    }

    /**
     * Creates a new user in the database.
     * <p>
     * Calls the USER_MGMT_PKG.CREATE_USER stored procedure. The procedure performs the following:
     * <ul>
     *   <li>Validates that username and email are unique</li>
     *   <li>Inserts the user record with generated ID</li>
     *   <li>Creates an audit log entry via LOG_MGMT_PKG</li>
     *   <li>Returns the generated user ID</li>
     * </ul>
     * <p>
     * The user's status defaults to PENDING if not explicitly set.
     * The createdBy field defaults to "SYSTEM" if not set.
     *
     * @param user the User object to create, must not be null. Required fields:
     *             username, email, passwordHash, firstName, lastName
     * @return the generated user ID
     * @throws DataIntegrityViolationException if username or email already exists
     * @throws DataAccessException if any other database error occurs
     * @see User
     */
    @Transactional
    @Override
    public Long createUser(User user) {
        log.info("Creating user: username='{}', email='{}', status={}",
                user.getUsername(), user.getEmail(), user.getStatus());

        var params = new MapSqlParameterSource()
                .addValue("p_username", user.getUsername())
                .addValue("p_email", user.getEmail())
                .addValue("p_password_hash", user.getPasswordHash())
                .addValue("p_first_name", user.getFirstName())
                .addValue("p_last_name", user.getLastName())
                .addValue("p_status", user.getStatus() != null ? user.getStatus().name() : "PENDING")
                .addValue("p_created_by", user.getCreatedBy() != null ? user.getCreatedBy() : "SYSTEM");

        Map<String, Object> result = spCreateUser.execute(params);

        // Check for errors from stored procedure
        String errorMsg = (String) result.get("o_err_msg");
        if (errorMsg != null && !errorMsg.isEmpty()) {
            log.error("CREATE_USER failed: {}", errorMsg);

            // Map to appropriate Spring exception
            if (errorMsg.contains("already taken") || errorMsg.contains("already registered")
                    || errorMsg.contains("Duplicate")) {
                throw new DataIntegrityViolationException(errorMsg);
            }
            throw new DataAccessException("Error creating user: " + errorMsg) {};
        }

        Long userId = ((Number) result.get("p_user_id")).longValue();
        log.info("User created successfully with id={}", userId);

        return userId;
    }

    /**
     * Updates an existing user's information.
     * <p>
     * Calls the USER_MGMT_PKG.UPDATE_USER stored procedure. The procedure performs the following:
     * <ul>
     *   <li>Validates that the user exists</li>
     *   <li>Validates that new email is unique (if changed)</li>
     *   <li>Updates the user record</li>
     *   <li>Updates the UPDATED_AT timestamp</li>
     *   <li>Creates an audit log entry via LOG_MGMT_PKG</li>
     * </ul>
     * <p>
     * The updatedBy field defaults to "SYSTEM" if not set.
     * The username cannot be changed after creation.
     *
     * @param user the User object with updated information, must not be null.
     *             The id field must be set to identify which user to update.
     *             Required fields: id, email, firstName, lastName, status
     * @throws DataIntegrityViolationException if the new email is already taken by another user
     * @throws DataAccessException if the user doesn't exist or any other database error occurs
     * @see User#getId()
     */
    @Transactional
    @Override
    public void updateUser(User user) {
        log.info("Updating user: id={}, email='{}', status={}",
                user.getId(), user.getEmail(), user.getStatus());

        var params = new MapSqlParameterSource()
                .addValue("p_user_id", user.getId())
                .addValue("p_email", user.getEmail())
                .addValue("p_first_name", user.getFirstName())
                .addValue("p_last_name", user.getLastName())
                .addValue("p_status", user.getStatus().name())
                .addValue("p_updated_by", user.getUpdatedBy() != null ? user.getUpdatedBy() : "SYSTEM");

        Map<String, Object> result = spUpdateUser.execute(params);

        // Check for errors from stored procedure
        String errorMsg = (String) result.get("o_err_msg");
        if (errorMsg != null && !errorMsg.isEmpty()) {
            log.error("UPDATE_USER failed: {}", errorMsg);

            // Map to appropriate Spring exception
            if (errorMsg.contains("already registered") || errorMsg.contains("Duplicate")) {
                throw new DataIntegrityViolationException(errorMsg);
            }
            if (errorMsg.contains("No user found") || errorMsg.contains("Failed to update")) {
                throw new DataAccessException("User not found with id=" + user.getId()) {};
            }
            throw new DataAccessException("Error updating user: " + errorMsg) {};
        }

        log.info("User updated successfully: id={}", user.getId());
    }

    /**
     * Builds a SimpleJdbcCall for stored procedures that return cursors.
     * <p>
     * This is a helper method that creates a properly configured SimpleJdbcCall
     * for query procedures that return result sets. All query procedures follow
     * the same pattern:
     * <ul>
     *   <li>Return a cursor named "p_cursor"</li>
     *   <li>Return an error message named "o_err_msg"</li>
     *   <li>Use the provided RowMapper to convert results to objects</li>
     * </ul>
     *
     * @param procedureName the procedure name within the package (e.g., "GET_USER_BY_ID")
     * @param mapper        the RowMapper to convert result set rows to objects
     * @return a configured SimpleJdbcCall ready for execution
     */
    private SimpleJdbcCall buildCall(String procedureName, RowMapper<?> mapper) {
        return new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("USER_MGMT_PKG")
                .withProcedureName(procedureName)
                .returningResultSet("p_cursor", mapper)
                .declareParameters(
                        new SqlOutParameter("o_err_msg", Types.VARCHAR)
                );
    }

    /**
     * Extracts a list of User objects from a stored procedure result map.
     * <p>
     * This helper method safely extracts the cursor result set from the
     * stored procedure output. If the cursor contains User objects, they
     * are returned as a list. If the cursor is empty or doesn't contain
     * User objects, an empty list is returned.
     *
     * @param result the result map from SimpleJdbcCall.execute()
     * @return a list of User objects, or empty list if no results
     */
    @SuppressWarnings("unchecked")
    private List<User> extractUsers(Map<String, Object> result) {
        Object cursor = result.get("p_cursor");
        if (cursor instanceof List<?> list) {
            if (!list.isEmpty() && list.get(0) instanceof User) {
                return (List<User>) list;
            }
        }
        return List.of();
    }

    /**
     * Checks for errors in stored procedure output and throws appropriate exceptions.
     * <p>
     * All stored procedures in USER_MGMT_PKG return an o_err_msg OUT parameter.
     * This method checks if an error message was returned and throws a
     * DataAccessException if one was found. This ensures that PL/SQL errors
     * are properly propagated to the Java layer.
     *
     * @param result the result map from SimpleJdbcCall.execute()
     * @param procedureName the name of the procedure that was called (for logging)
     * @throws DataAccessException if an error message was returned by the stored procedure
     */
    private void checkForErrors(Map<String, Object> result, String procedureName) {
        String errorMsg = (String) result.get("o_err_msg");
        if (errorMsg != null && !errorMsg.isEmpty()) {
            log.error("{} returned error: {}", procedureName, errorMsg);
            throw new DataAccessException("Error executing " + procedureName + ": " + errorMsg) {};
        }
    }
}