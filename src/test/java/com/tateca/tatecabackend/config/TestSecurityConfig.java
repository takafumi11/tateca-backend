package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.security.ApiKeyAuthentication;
import com.tateca.tatecabackend.security.FirebaseAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Test configuration that bypasses real authentication.
 * Uses path-based authentication: /internal/** -> ApiKeyAuthentication, others -> FirebaseAuthentication
 */
@TestConfiguration
public class TestSecurityConfig {

    public static final String TEST_UID = "test-user-uid";
    public static final String TEST_API_KEY = "test-lambda-api-key";

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain filterChain) throws ServletException, IOException {
                    String path = request.getRequestURI();

                    // Determine authentication type by path
                    Authentication authentication;
                    if (path.startsWith("/internal/")) {
                        authentication = new ApiKeyAuthentication();
                    } else {
                        authentication = new FirebaseAuthentication(TEST_UID);
                    }

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    try {
                        filterChain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }
            }, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
