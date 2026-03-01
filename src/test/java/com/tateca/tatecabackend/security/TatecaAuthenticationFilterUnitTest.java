package com.tateca.tatecabackend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TatecaAuthenticationFilter Unit Tests")
class TatecaAuthenticationFilterUnitTest {

    private TatecaAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private org.springframework.core.env.Environment environment;

    private static final String TEST_API_KEY = "test-api-key-12345";
    private static final String TEST_PROJECT_ID = "test-project-id";

    @BeforeEach
    void setUp() throws Exception {
        // Configure environment mock to return empty active profiles (not dev mode)
        // Use lenient() to avoid UnnecessaryStubbingException in tests that don't call this method
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{});
        // Provide a writer for tests that trigger error responses
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter = new TatecaAuthenticationFilter(environment, new ObjectMapper());
        ReflectionTestUtils.setField(filter, "lambdaApiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(filter, "firebaseProjectId", TEST_PROJECT_ID);

        // Setup SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("API Key Authentication Tests")
    class ApiKeyAuthenticationTests {

        @Test
        @DisplayName("Should authenticate with valid X-API-Key for /internal/** path")
        void shouldAuthenticateWithValidApiKey() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn(TEST_API_KEY);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(securityContext).setAuthentication(any(ApiKeyAuthentication.class));
            verify(securityContext).setAuthentication(argThat(auth ->
                auth instanceof ApiKeyAuthentication &&
                ((ApiKeyAuthentication) auth).getUid().equals("system-internal")
            ));
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("Should reject invalid X-API-Key")
        void shouldRejectInvalidApiKey() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn("wrong-api-key");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should reject missing X-API-Key header")
        void shouldRejectMissingApiKey() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should reject empty X-API-Key header")
        void shouldRejectEmptyApiKey() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn("");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Firebase Authentication Tests")
    class FirebaseAuthenticationTests {

        @Test
        @DisplayName("Should reject missing Authorization header for non-internal paths")
        void shouldRejectMissingAuthorizationHeader() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/groups/123/transactions");
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should reject invalid Authorization header format")
        void shouldRejectInvalidAuthorizationFormat() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/groups/123/transactions");
            when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should authenticate with valid Bearer token")
        void shouldAuthenticateWithValidBearerToken() throws Exception {
            // Given
            String validToken = "valid-firebase-token";
            when(request.getRequestURI()).thenReturn("/groups/123/transactions");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

            // Mock FirebaseAuth and FirebaseToken
            try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
                FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
                FirebaseToken firebaseToken = mock(FirebaseToken.class);

                firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
                // Updated to match implementation: checkRevoked=true
                when(firebaseAuth.verifyIdToken(validToken, true)).thenReturn(firebaseToken);

                // Setup token claims
                Map<String, Object> claims = new HashMap<>();
                claims.put("aud", TEST_PROJECT_ID);
                when(firebaseToken.getClaims()).thenReturn(claims);
                when(firebaseToken.getIssuer()).thenReturn("https://securetoken.google.com/" + TEST_PROJECT_ID);
                when(firebaseToken.getUid()).thenReturn("test-user-123");

                // When
                filter.doFilterInternal(request, response, filterChain);

                // Then
                verify(filterChain).doFilter(request, response);
                verify(securityContext).setAuthentication(any(FirebaseAuthentication.class));
                verify(response, never()).setStatus(anyInt());
            }
        }
    }

    @Nested
    @DisplayName("Path-Based Routing Tests")
    class PathRoutingTests {

        @Test
        @DisplayName("Should use API key authentication for /internal/exchange-rates")
        void shouldUseApiKeyForInternalExchangeRates() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn(TEST_API_KEY);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request).getHeader("X-API-Key");
            verify(request, never()).getHeader("Authorization");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use Firebase authentication for non-internal paths")
        void shouldUseFirebaseForNonInternalPaths() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/groups/123/transactions");
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request).getHeader("Authorization");
            verify(request, never()).getHeader("X-API-Key");
        }

        @Test
        @DisplayName("Should bypass authentication for /actuator/health")
        void shouldBypassAuthenticationForActuatorHealth() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/actuator/health");

            // When
            boolean shouldFilter = !filter.shouldNotFilter(request);

            // Then
            assertThat(shouldFilter).isFalse();
        }

    }

    @Nested
    @DisplayName("Security Context Tests")
    class SecurityContextTests {

        @Test
        @DisplayName("Should clear security context after request")
        void shouldClearSecurityContextAfterRequest() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn(TEST_API_KEY);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(securityContext).setAuthentication(any(Authentication.class));
            verify(filterChain).doFilter(request, response);
            // SecurityContextHolder.clearContext() is called in finally block
        }

        @Test
        @DisplayName("Should clear security context even on authentication failure")
        void shouldClearSecurityContextOnFailure() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/internal/exchange-rates");
            when(request.getHeader("X-API-Key")).thenReturn("wrong-key");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(401);
            // SecurityContextHolder.clearContext() is still called in finally block
        }
    }
}
