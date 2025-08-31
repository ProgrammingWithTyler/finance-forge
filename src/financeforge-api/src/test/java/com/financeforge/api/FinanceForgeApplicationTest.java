package com.financeforge.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test to verify FinanceForge application starts correctly
 */
@SpringBootTest
@ActiveProfiles("dev")
class FinanceForgeApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // If this passes, it means all the beans are wired correctly
    }
}