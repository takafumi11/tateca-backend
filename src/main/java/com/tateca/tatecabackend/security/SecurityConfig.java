package com.tateca.tatecabackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Tateca Backend.
 *
 * Security Architecture:
 * - Stateless JWT-based authentication
 * - Two authentication methods: Firebase JWT (users) and API Key (Lambda)
 * - No authorization layer (trusts UID from client)
 * - CORS enabled for iOS client
 * - Security headers for production
 * - Dev mode: x-uid header bypass (dev profile only)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Stateless session management (JWT-based)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/error",
                    "/actuator/health"
                ).permitAll()

                // Dev endpoints (will be disabled in production via @Profile)
                .requestMatchers("/dev/**").permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Add custom authentication filter
            .addFilterBefore(tatecaAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

            // Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable())
                .contentTypeOptions(cto -> cto.disable())
            );

        return http.build();
    }

    @Bean
    public TatecaAuthenticationFilter tatecaAuthenticationFilter() {
        return new TatecaAuthenticationFilter(environment);
    }
}
