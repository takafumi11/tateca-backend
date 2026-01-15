package com.tateca.tatecabackend.actuator;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Actuator health check endpoint.
 *
 * Verifies that /actuator/health is publicly accessible without authentication
 * for cold sleep prevention and monitoring purposes.
 */
@AutoConfigureMockMvc
@DisplayName("Actuator Health Check Integration Tests")
class ActuatorHealthCheckIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Given application is running")
    class WhenApplicationIsRunning {

        @Test
        @DisplayName("Then health endpoint should be accessible without authentication")
        void thenHealthEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
            // When: Accessing health endpoint without authentication
            mockMvc.perform(get("/actuator/health"))
                    // Then: Should return 200 OK
                    .andExpect(status().isOk())
                    // And: Should return JSON with status field
                    .andExpect(jsonPath("$.status").exists())
                    // And: Status should be UP
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("Then health endpoint should not expose detailed information")
        void thenHealthEndpointShouldNotExposeDetailedInformation() throws Exception {
            // When: Accessing health endpoint
            mockMvc.perform(get("/actuator/health"))
                    // Then: Should return 200 OK
                    .andExpect(status().isOk())
                    // And: Should not contain components details (management.endpoint.health.show-details=never)
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Given security configuration")
    class WhenVerifyingSecurityConfiguration {

        @Test
        @DisplayName("Then protected endpoints should require authentication")
        void thenProtectedEndpointsShouldRequireAuthentication() throws Exception {
            // When: Accessing protected endpoint without authentication
            mockMvc.perform(get("/groups"))
                    // Then: Should return 401 Unauthorized (missing authentication)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then health endpoint should remain accessible without authentication")
        void thenHealthEndpointShouldRemainAccessibleWithoutAuthentication() throws Exception {
            // When: Accessing health endpoint without authentication
            mockMvc.perform(get("/actuator/health"))
                    // Then: Should return 200 OK (bypasses authentication filter)
                    .andExpect(status().isOk());
        }
    }
}
