package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for Controller integration tests with full Spring context and MockMvc.
 * Tests REST endpoints end-to-end with real database and mocked Firebase authentication.
 *
 * <p>Note: Integration tests are executed in the same thread to avoid
 * connection pool issues with the shared MySQL container.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@SuppressWarnings("resource")
public abstract class AbstractControllerIntegrationTest extends AbstractIntegrationTest {

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

}
