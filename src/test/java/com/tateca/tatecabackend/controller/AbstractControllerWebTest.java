package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for Controller web tests using @WebMvcTest.
 * Provides MockMvc, ObjectMapper, and mocked Firebase authentication.
 */
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class AbstractControllerWebTest {

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
}
