package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.interceptor.BearerTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static com.tateca.tatecabackend.constants.AttributeConstants.UID_ATTRIBUTE;

/**
 * Test configuration for Lambda API key authentication testing.
 * Allows proper testing of X-API-KEY authentication without Firebase.
 *
 * <p>This configuration overrides the default {@link BearerTokenInterceptor}
 * to enable testing of Lambda API key validation logic including:
 * <ul>
 *   <li>Valid API key authentication</li>
 *   <li>Invalid API key rejection (401 Unauthorized)</li>
 *   <li>Missing API key handling (400 Bad Request)</li>
 * </ul>
 */
@TestConfiguration
public class TestLambdaSecurityConfig {

    /**
     * Test Lambda API key used for authentication testing.
     */
    public static final String TEST_LAMBDA_API_KEY = "test-lambda-api-key";

    private static final String X_API_KEY_HEADER = "X-API-KEY";
    private static final String LAMBDA_UID = "lambda-system";

    /**
     * Creates a test interceptor that validates Lambda API keys.
     *
     * @return BearerTokenInterceptor configured for Lambda API key testing
     */
    @Bean
    @Primary
    public BearerTokenInterceptor testLambdaBearerTokenInterceptor() {
        return new BearerTokenInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object handler) {
                String apiKey = request.getHeader(X_API_KEY_HEADER);

                // Check if X-API-KEY header is present
                if (apiKey == null || apiKey.isEmpty()) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing authentication header (X-API-KEY or Authorization)"
                    );
                }

                // Validate X-API-KEY value
                if (!TEST_LAMBDA_API_KEY.equals(apiKey)) {
                    throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid X-API-KEY"
                    );
                }

                // Set Lambda system UID for authenticated requests
                request.setAttribute(UID_ATTRIBUTE, LAMBDA_UID);
                return true;
            }
        };
    }
}
