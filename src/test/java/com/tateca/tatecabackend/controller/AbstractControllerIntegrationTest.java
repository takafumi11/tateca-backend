package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for Controller integration tests with full Spring context and MockMvc.
 * Tests REST endpoints end-to-end with real database and mocked Firebase authentication.
 *
 * <p>Note: Integration tests are executed in the same thread to avoid
 * connection pool issues with the shared MySQL container.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@SuppressWarnings("resource")
public abstract class AbstractControllerIntegrationTest {

    @ServiceConnection
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    static {
        mysql.start();
    }

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * MockMvc for simulating HTTP requests.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * ObjectMapper for JSON serialization/deserialization.
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Test UID used for authenticated requests.
     */
    protected static final String TEST_UID = TestSecurityConfig.TEST_UID;

    /**
     * Converts an object to JSON string for request bodies.
     */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Parses JSON string to an object.
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

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
