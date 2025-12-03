package com.tateca.tatecabackend;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

/**
 * Base class for Integration tests with MySQL and WireMock containers.
 * Provides database and external API mocking infrastructure.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    protected static WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:3.3.1")
            .withReuse(true);

    static {
        mysql.start();
        wireMock.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("exchange.rate.base-url", wireMock::getBaseUrl);
    }

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Flushes pending changes to the database.
     */
    protected void flush() {
        entityManager.flush();
    }

    /**
     * Clears the persistence context, detaching all entities.
     */
    protected void clear() {
        entityManager.clear();
    }

    /**
     * Flushes and clears the persistence context.
     */
    protected void flushAndClear() {
        flush();
        clear();
    }
}
