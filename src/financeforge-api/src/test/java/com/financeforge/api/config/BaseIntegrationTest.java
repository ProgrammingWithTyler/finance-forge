package com.financeforge.api.config;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers.
 *
 * All repository tests should extend this class to get:
 * - Real Oracle database running in Docker
 * - Automatic cleanup after tests
 * - Flyway migrations applied
 * - Helper method for Oracle database-generated values
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseIntegrationTest {

    @Autowired
    protected EntityManager entityManager;

    /**
     * Oracle container that runs for all tests.
     *
     * IMPORTANT: This uses Oracle XE (Express Edition).
     * - Docker must be running on your machine
     * - First run will download Oracle image (takes 5-10 minutes)
     * - Subsequent runs are fast (container reuse)
     */
    @Container
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);  // Reuse container across test runs

    /**
     * Override Spring datasource properties with Testcontainers values.
     */
    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracleContainer::getJdbcUrl);
        registry.add("spring.datasource.username", oracleContainer::getUsername);
        registry.add("spring.datasource.password", oracleContainer::getPassword);
    }

    /**
     * Save entity and refresh to get Oracle-generated values.
     *
     * USE THIS METHOD for all saves in tests when you need Oracle-generated
     * values (timestamps, sequences, triggers, defaults, etc.)
     *
     * WHY? Because Oracle generates values on INSERT/UPDATE, but Hibernate
     * doesn't automatically fetch them back. We must explicitly refresh.
     *
     * @param repository The JPA repository to save with
     * @param entity The entity to save
     * @return The saved entity with Oracle-generated values populated
     */
    protected <T, ID> T saveAndRefresh(JpaRepository<T, ID> repository, T entity) {
        T saved = repository.saveAndFlush(entity);
        entityManager.refresh(saved);
        return saved;
    }
}