package com.financeforge.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToOracleDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();

            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Connected to: " + metaData.getDatabaseProductName());
            System.out.println("Version: " + metaData.getDatabaseProductVersion());
            System.out.println("User: " + metaData.getUserName());
        }
    }

    @Test
    void shouldReturnOracleVersion() {
        String version = jdbcTemplate.queryForObject(
                "SELECT BANNER FROM V$VERSION WHERE ROWNUM = 1",
                String.class
        );
        assertThat(version).contains("Oracle");
        System.out.println("Database version: " + version);
    }

    @Test
    void shouldAccessFinanceForgeSchema() {
        // Test that we can access our schema
        String currentUser = jdbcTemplate.queryForObject(
                "SELECT USER FROM DUAL",
                String.class
        );
        assertThat(currentUser).isEqualToIgnoringCase("FINANCEFORGE");
        System.out.println("Connected as user: " + currentUser);
    }
}