package com.financeforge.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints for monitoring FinanceForge API status
 */
@RestController
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("application", "FinanceForge API");
        healthInfo.put("version", "1.0.0-SNAPSHOT");
        healthInfo.put("java", System.getProperty("java.version"));

        // Database health check
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                Map<String, Object> dbInfo = getStringObjectMap(connection);
                healthInfo.put("database", dbInfo);
            } catch (Exception e) {
                Map<String, Object> dbInfo = new HashMap<>();
                dbInfo.put("status", "ERROR");
                dbInfo.put("error", e.getMessage());
                healthInfo.put("database", dbInfo);
            }
        } else {
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("status", "NOT_CONFIGURED");
            dbInfo.put("message", "DataSource not available - database auto-configuration may be disabled");
            healthInfo.put("database", dbInfo);
        }

        return healthInfo;
    }

    private static Map<String, Object> getStringObjectMap(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("status", "CONNECTED");
        dbInfo.put("url", metaData.getURL());
        dbInfo.put("product", metaData.getDatabaseProductName());
        dbInfo.put("version", metaData.getDatabaseProductVersion());
        dbInfo.put("driver", metaData.getDriverName());
        dbInfo.put("driverVersion", metaData.getDriverVersion());
        return dbInfo;
    }


    @GetMapping("/")
    public Map<String, String> welcome() {
        return Map.of(
                "message", "Welcome to FinanceForge API",
                "documentation", "/swagger-ui.html",
                "health", "/health"
        );
    }
}