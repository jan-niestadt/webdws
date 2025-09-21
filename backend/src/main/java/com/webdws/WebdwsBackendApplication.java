package com.webdws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * WebDWS Backend Application - Main Spring Boot Application Entry Point
 * 
 * This is the main application class that:
 * - Bootstraps the Spring Boot application
 * - Enables configuration properties for external configuration
 * - Provides the entry point for the XML document management backend
 * - Integrates with eXist-db for XML document storage and retrieval
 */
@SpringBootApplication
@EnableConfigurationProperties
public class WebdwsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebdwsBackendApplication.class, args);
    }
}
