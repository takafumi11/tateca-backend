package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.interceptor.BearerTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static com.tateca.tatecabackend.constants.AttributeConstants.UID_ATTRIBUTE;

/**
 * Test configuration that bypasses Firebase authentication.
 * Replaces BearerTokenInterceptor with a mock that always sets TEST_UID.
 */
@TestConfiguration
public class TestSecurityConfig {

    public static final String TEST_UID = "test-user-uid";

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
