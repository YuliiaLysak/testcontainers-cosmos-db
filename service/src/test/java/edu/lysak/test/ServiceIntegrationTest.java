package edu.lysak.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceIntegrationTest extends ContainerizedEnvironment {

    @Test
    void containers_shouldBeRunning() {
        assertTrue(cosmos.isRunning());
    }
}
