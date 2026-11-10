package com.dsecap.predictor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.CommandLineRunner;

/**
 * Test configuration to override beans for integration testing.
 * Prevents the main CommandLineRunner from executing during tests.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Override the CommandLineRunner to prevent main application execution during tests.
     */
    @Bean
    @Primary
    public CommandLineRunner testCommandLineRunner() {
        return args -> {
            // Do nothing - this prevents the main application from running during tests
        };
    }
}