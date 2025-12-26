package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.security.LambdaAuthentication;
import com.tateca.tatecabackend.security.TatecaAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Test configuration for Lambda API key authentication testing.
 * Allows proper testing of X-API-KEY authentication without Firebase.
 *
 * <p>This configuration enables testing of Lambda API key validation logic including:
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

    @Bean
    @Primary
    public SecurityFilterChain testLambdaSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain filterChain) throws ServletException, IOException {
                    String apiKey = request.getHeader(X_API_KEY_HEADER);

                    try {
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

                        // Set Lambda authentication in Security Context
                        LambdaAuthentication authentication = new LambdaAuthentication();
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        filterChain.doFilter(request, response);
                    } catch (ResponseStatusException e) {
                        response.sendError(e.getStatusCode().value(), e.getReason());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }
            }, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Primary
    public TatecaAuthenticationFilter testTatecaAuthenticationFilter() {
        // Return a no-op filter since the test filter above handles authentication
        return new TatecaAuthenticationFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain filterChain) throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }
}
