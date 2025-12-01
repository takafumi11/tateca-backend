package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.service.FirebaseService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for mocking beans that require external services.
 *
 * <p>This configuration provides mock implementations of services that depend on
 * external resources (like Firebase) which are not available or needed during testing.</p>
 *
 * <p>Usage: Import this configuration in test classes or extend AbstractIntegrationTest
 * which automatically includes this configuration.</p>
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a mock implementation of FirebaseService for testing.
     *
     * <p>This bean is marked as @Primary to override the real FirebaseService bean
     * during test execution. The mock skips Firebase initialization which requires
     * valid credentials and network access.</p>
     *
     * @return a mocked FirebaseService instance
     */
    @Bean
    @Primary
    public FirebaseService firebaseServiceMock() {
        FirebaseService mock = Mockito.mock(FirebaseService.class);
        // Mock does not execute @PostConstruct initialization
        // Specific behaviors can be stubbed in individual tests as needed:
        // when(mock.generateCustomToken(anyString())).thenReturn("mock-token");
        return mock;
    }
}
