package com.financeforge.api.repository;

import com.financeforge.api.config.BaseIntegrationTest;
import com.financeforge.api.domain.model.User;
import com.financeforge.api.domain.model.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserRepository using Testcontainers with Oracle XE.
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */
public class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = null;
    }

    @AfterEach
    void cleanUp() {
        if (testUserId != null) {
            try {
                Optional<User> user = userRepository.findById(testUserId);
                if (user.isPresent()) {
                    User u = user.get();
                    u.setStatus(UserStatus.INACTIVE);
                    u.setUpdatedBy("TEST_CLEANUP");
                    userRepository.updateUser(u);
                }
            } catch (DataAccessException e) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Generate unique username to prevent test interference.
     * Appends timestamp to ensure uniqueness across test runs.
     */
    private String uniqueUsername(String base) {
        return base + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique email to prevent test interference.
     */
    private String uniqueEmail(String base) {
        return base + "_" + System.currentTimeMillis() + "@financeforge.com";
    }

    /**
     * Helper method to create a test user with hashed password and unique identifiers.
     */
    private User createTestUser(String usernameBase, String emailBase, String password) {
        return User.builder()
            .username(uniqueUsername(usernameBase))
            .email(uniqueEmail(emailBase))
            .passwordHash(passwordEncoder.encode(password))
            .firstName("Test")
            .lastName("User")
            .status(UserStatus.PENDING)
            .createdBy("TEST")
            .build();
    }

    // ==================== CREATE USER TESTS ====================

    @Test
    void shouldCreateUserAndRetrieveById() {
        // ARRANGE
        User user = createTestUser("johndoe", "john", "Password123!");

        // ACT
        Long userId = userRepository.createUser(user);
        testUserId = userId;
        Optional<User> retrieved = userRepository.findById(userId);

        // ASSERT
        assertThat(retrieved).isPresent();
        User savedUser = retrieved.get();
        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getUsername()).startsWith("johndoe_");
        assertThat(savedUser.getEmail()).startsWith("john_");
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(savedUser.getPasswordHash()).isNotNull();
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        assertThat(savedUser.getCreatedBy()).isEqualTo("TEST");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldCreateUserWithDefaultStatus() {
        // ARRANGE
        User user = User.builder()
            .username(uniqueUsername("defaultstatus"))
            .email(uniqueEmail("default"))
            .passwordHash(passwordEncoder.encode("Password123!"))
            .firstName("Default")
            .lastName("Status")
            .createdBy("TEST")
            .build();

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void shouldCreateUserWithActiveStatus() {
        // ARRANGE
        User user = createTestUser("activeuser", "active", "Password123!");
        user.setStatus(UserStatus.ACTIVE);

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.isActive()).isTrue();
    }

    // ==================== DUPLICATE VALIDATION TESTS ====================

    @Test
    void shouldThrowOnDuplicateUsername() {
        // ARRANGE - Create first user with specific username
        String sharedUsername = uniqueUsername("duplicate");
        User firstUser = User.builder()
            .username(sharedUsername)
            .email(uniqueEmail("user1"))
            .passwordHash(passwordEncoder.encode("Password123!"))
            .firstName("Test")
            .lastName("User")
            .createdBy("TEST")
            .build();

        testUserId = userRepository.createUser(firstUser);

        // ARRANGE - Try to create user with same username but different email
        User duplicateUser = User.builder()
            .username(sharedUsername)  // SAME username
            .email(uniqueEmail("user2"))  // Different email
            .passwordHash(passwordEncoder.encode("Password123!"))
            .firstName("Test")
            .lastName("User")
            .createdBy("TEST")
            .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> userRepository.createUser(duplicateUser))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("already taken");
    }

    @Test
    void shouldThrowOnDuplicateEmail() {
        // ARRANGE - Create first user with specific email
        String sharedEmail = uniqueEmail("duplicate");
        User firstUser = User.builder()
            .username(uniqueUsername("user1"))
            .email(sharedEmail)
            .passwordHash(passwordEncoder.encode("Password123!"))
            .firstName("Test")
            .lastName("User")
            .createdBy("TEST")
            .build();

        testUserId = userRepository.createUser(firstUser);

        // ARRANGE - Try to create user with same email but different username
        User duplicateUser = User.builder()
            .username(uniqueUsername("user2"))  // Different username
            .email(sharedEmail)  // SAME email
            .passwordHash(passwordEncoder.encode("Password123!"))
            .firstName("Test")
            .lastName("User")
            .createdBy("TEST")
            .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> userRepository.createUser(duplicateUser))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("already registered");
    }

    // ==================== FIND BY USERNAME TESTS ====================

    @Test
    void shouldFindUserByUsername() {
        // ARRANGE
        User user = createTestUser("janedoe", "jane", "Password123!");
        testUserId = userRepository.createUser(user);

        // ACT - Search by the exact username that was created
        Optional<User> found = userRepository.findByUsername(user.getUsername());

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testUserId);
        assertThat(found.get().getUsername()).isEqualTo(user.getUsername());
        assertThat(found.get().getPasswordHash()).isNotNull();
    }

    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        // ACT
        Optional<User> found = userRepository.findByUsername("nonexistent_user_12345");

        // ASSERT
        assertThat(found).isEmpty();
    }

    // ==================== FIND BY EMAIL TESTS ====================

    @Test
    void shouldFindUserByEmail() {
        // ARRANGE
        User user = createTestUser("bobsmith", "bob", "Password123!");
        testUserId = userRepository.createUser(user);

        // ACT - Search by the exact email that was created
        Optional<User> found = userRepository.findByEmail(user.getEmail());

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testUserId);
        assertThat(found.get().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        // ACT
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // ASSERT
        assertThat(found).isEmpty();
    }

    // ==================== FIND BY ID TESTS ====================

    @Test
    void shouldReturnEmptyWhenUserIdNotFound() {
        // ACT
        Optional<User> found = userRepository.findById(999999L);

        // ASSERT
        assertThat(found).isEmpty();
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    void shouldUpdateUserSuccessfully() throws InterruptedException {
        // ARRANGE
        User user = createTestUser("updatetest", "update", "Password123!");
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        Thread.sleep(1100);

        // ACT
        savedUser.setFirstName("Modified");
        savedUser.setLastName("Name");
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setUpdatedBy("TEST_UPDATER");
        userRepository.updateUser(savedUser);

        User updatedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(updatedUser.getFirstName()).isEqualTo("Modified");
        assertThat(updatedUser.getLastName()).isEqualTo("Name");
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updatedUser.getUpdatedBy()).isEqualTo("TEST_UPDATER");
        assertThat(updatedUser.getUpdatedAt()).isAfter(savedUser.getUpdatedAt());
        assertThat(updatedUser.getCreatedAt()).isEqualTo(savedUser.getCreatedAt());
    }

    @Test
    void shouldUpdateEmailSuccessfully() {
        // ARRANGE
        User user = createTestUser("emailchange", "old", "Password123!");
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ACT
        String newEmail = uniqueEmail("new");
        savedUser.setEmail(newEmail);
        userRepository.updateUser(savedUser);

        User updatedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
    }

    @Test
    void shouldThrowWhenUpdatingToExistingEmail() {
        // ARRANGE - Create first user
        User firstUser = createTestUser("user1", "first", "Password123!");
        Long firstUserId = userRepository.createUser(firstUser);

        // ARRANGE - Create second user
        User secondUser = createTestUser("user2", "second", "Password123!");
        testUserId = userRepository.createUser(secondUser);

        // ACT & ASSERT - Try to update second user's email to first user's email
        User userToUpdate = userRepository.findById(testUserId).orElseThrow();
        userToUpdate.setEmail(firstUser.getEmail());

        assertThatThrownBy(() -> userRepository.updateUser(userToUpdate))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("already registered");

        // Cleanup first user
        User cleanup = userRepository.findById(firstUserId).orElseThrow();
        cleanup.setStatus(UserStatus.INACTIVE);
        userRepository.updateUser(cleanup);
    }

    // ==================== FIND BY STATUS TESTS ====================

    @Test
    void shouldFindUsersByStatus() {
        // ARRANGE - Create active user
        User activeUser = createTestUser("activeuser", "active", "Password123!");
        activeUser.setStatus(UserStatus.ACTIVE);
        Long activeUserId = userRepository.createUser(activeUser);

        // ARRANGE - Create pending user
        User pendingUser = createTestUser("pendinguser", "pending", "Password123!");
        pendingUser.setStatus(UserStatus.PENDING);
        testUserId = userRepository.createUser(pendingUser);

        // ACT
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        List<User> pendingUsers = userRepository.findByStatus(UserStatus.PENDING);

        // ASSERT
        assertThat(activeUsers)
            .isNotEmpty()
            .anyMatch(u -> u.getUsername().equals(activeUser.getUsername()));

        assertThat(pendingUsers)
            .isNotEmpty()
            .anyMatch(u -> u.getUsername().equals(pendingUser.getUsername()));

        // Cleanup active user
        User cleanup = userRepository.findById(activeUserId).orElseThrow();
        cleanup.setStatus(UserStatus.INACTIVE);
        userRepository.updateUser(cleanup);
    }

    @Test
    void shouldFindAllActiveUsers() {
        // ARRANGE
        User activeUser = createTestUser("findactive", "findactive", "Password123!");
        activeUser.setStatus(UserStatus.ACTIVE);
        testUserId = userRepository.createUser(activeUser);

        // ACT
        List<User> activeUsers = userRepository.findAllActiveUsers();

        // ASSERT
        assertThat(activeUsers)
            .isNotEmpty()
            .anyMatch(u -> u.getUsername().equals(activeUser.getUsername()))
            .allMatch(u -> u.getStatus() == UserStatus.ACTIVE);
    }

    // ==================== PASSWORD HASH TESTS ====================

    @Test
    void shouldStorePasswordAsHash() {
        // ARRANGE
        String plainPassword = "MySecretPassword123!";
        User user = createTestUser("hashtest", "hash", plainPassword);

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(plainPassword);
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        assertThat(savedUser.getPasswordHash()).hasSize(60);
        assertThat(passwordEncoder.matches(plainPassword, savedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", savedUser.getPasswordHash())).isFalse();
    }

    // ==================== FULL NAME TESTS ====================

    @Test
    void shouldReturnCorrectFullName() {
        // ARRANGE
        User user = createTestUser("nametest", "name", "Password123!");
        user.setFirstName("John");
        user.setLastName("Doe");

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.getFullName()).isEqualTo("John Doe");
    }

    // ==================== AUDIT FIELDS TESTS ====================

    @Test
    void shouldSetAuditFieldsOnCreate() {
        // ARRANGE
        User user = createTestUser("auditcreate", "auditcreate", "Password123!");
        user.setCreatedBy("REGISTRATION");

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.getCreatedBy()).isEqualTo("REGISTRATION");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        // Oracle defaults updated_by to 'SYSTEM' on INSERT via DEFAULT constraint
        assertThat(savedUser.getUpdatedBy()).isEqualTo("REGISTRATION");
    }

    @Test
    void shouldSetAuditFieldsOnUpdate() throws InterruptedException {
        // ARRANGE
        User user = createTestUser("auditupdate", "auditupdate", "Password123!");
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        Thread.sleep(1100);

        // ACT
        savedUser.setFirstName("Updated");
        savedUser.setUpdatedBy("ADMIN");
        userRepository.updateUser(savedUser);

        User updatedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(updatedUser.getUpdatedBy()).isEqualTo("ADMIN");
        assertThat(updatedUser.getUpdatedAt()).isAfter(savedUser.getUpdatedAt());
    }

    // ==================== USER STATUS TESTS ====================

    @Test
    void shouldIdentifyActiveUser() {
        // ARRANGE
        User user = createTestUser("statusactive", "statusactive", "Password123!");
        user.setStatus(UserStatus.ACTIVE);

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.isActive()).isTrue();
    }

    @Test
    void shouldIdentifyInactiveUser() {
        // ARRANGE
        User user = createTestUser("statusinactive", "statusinactive", "Password123!");
        user.setStatus(UserStatus.INACTIVE);

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.isActive()).isFalse();
    }

    @Test
    void shouldIdentifyPendingUser() {
        // ARRANGE
        User user = createTestUser("statuspending", "statuspending", "Password123!");
        user.setStatus(UserStatus.PENDING);

        // ACT
        testUserId = userRepository.createUser(user);
        User savedUser = userRepository.findById(testUserId).orElseThrow();

        // ASSERT
        assertThat(savedUser.isActive()).isFalse();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
    }
}