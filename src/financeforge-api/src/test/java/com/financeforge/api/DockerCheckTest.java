package com.financeforge.api;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

public class DockerCheckTest {

    @Test
    void testDockerWorks() {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:latest").withCommand("echo", "hello")) {
            container.start();
            System.out.println("Container running: " + container.isRunning());
        }
    }
}
