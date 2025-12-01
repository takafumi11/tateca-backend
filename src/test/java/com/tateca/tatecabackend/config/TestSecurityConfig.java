package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.interceptor.BearerTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static com.tateca.tatecabackend.constants.AttributeConstants.UID_ATTRIBUTE;

/**
 * Test configuration for bypassing Firebase authentication in tests.
 *
 * <p>This configuration replaces the production {@link BearerTokenInterceptor}
 * with a test implementation that always returns a fixed test UID.</p>
 *
 * <p><strong>Why is this needed?</strong></p>
 * <ul>
 *   <li>Firebase authentication requires valid JWT tokens</li>
 *   <li>Controller tests should focus on business logic, not authentication</li>
 *   <li>Real Firebase tokens are difficult to generate in tests</li>
 *   <li>Tests need a predictable, stable UID</li>
 * </ul>
 *
 * <p><strong>How it works:</strong></p>
 * <ul>
 *   <li>@Primary bean replaces production BearerTokenInterceptor</li>
 *   <li>Always sets request attribute to "test-user-uid"</li>
 *   <li>@UId annotation in controllers will receive this test UID</li>
 *   <li>No Firebase validation is performed</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <ul>
 *   <li>Imported automatically by {@link AbstractControllerTest}</li>
 *   <li>All controller integration tests use this test UID</li>
 *   <li>Tests can use TEST_UID constant from AbstractControllerTest</li>
 * </ul>
 *
 * <p><strong>Important Notes:</strong></p>
 * <ul>
 *   <li>This bypasses authentication ONLY in test profile</li>
 *   <li>Production authentication remains unchanged</li>
 *   <li>Do NOT use this configuration in production code</li>
 * </ul>
 *
 * @see AbstractControllerTest
 * @see BearerTokenInterceptor
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Test UID that will be used for all authenticated requests in tests.
     */
    public static final String TEST_UID = "test-user-uid";

    /**
     * Creates a test implementation of BearerTokenInterceptor.
     *
     * <p>This bean is marked as @Primary, so it replaces the production
     * BearerTokenInterceptor when running tests.</p>
     *
     * <p>The test implementation:</p>
     * <ul>
     *   <li>Always returns true (allows all requests)</li>
     *   <li>Sets UID_ATTRIBUTE to TEST_UID</li>
     *   <li>No Firebase authentication is performed</li>
     * </ul>
     *
     * @return a test BearerTokenInterceptor that bypasses authentication
     */
    @Bean
    @Primary
    public BearerTokenInterceptor testBearerTokenInterceptor() {
        return new BearerTokenInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                // Always set test UID without Firebase validation
                request.setAttribute(UID_ATTRIBUTE, TEST_UID);
                return true;
            }
        };
    }
}
