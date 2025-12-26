package com.tateca.tatecabackend.security;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security filter that handles multiple authentication methods:
 * - API Key authentication for internal endpoints (/internal/**)
 * - Firebase JWT authentication for user endpoints
 */
public class TatecaAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TatecaAuthenticationFilter.class);
    private static final String X_API_KEY_HEADER = "X-API-Key";

    @Value("${firebase.project.id}")
    private String firebaseProjectId;

    @Value("${lambda.api.key}")
    private String lambdaApiKey;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/error",
        "/swagger-ui",
        "/v3/api-docs",
        "/swagger-resources",
        "/webjars",
        "/actuator/health",
        "/dev"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        try {
            // Route to appropriate authentication method based on path
            if (path.startsWith("/internal/")) {
                authenticateWithApiKey(request);
            } else {
                authenticateWithFirebaseToken(request);
            }

            filterChain.doFilter(request, response);

        } catch (ApiKeyAuthenticationException e) {
            logAuthenticationFailure("API Key", path, e.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API Key");
        } catch (FirebaseAuthException e) {
            logAuthenticationFailure("Firebase", path, e.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid Firebase authentication token");
        } catch (ResponseStatusException e) {
            logAuthenticationFailure("General", path, e.getReason());
            response.sendError(e.getStatusCode().value(), e.getReason());
        } finally {
            // Clear security context after request
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateWithApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader(X_API_KEY_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new ApiKeyAuthenticationException("Missing X-API-Key header");
        }

        // SECURITY: Use constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                lambdaApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiKeyAuthenticationException("Invalid X-API-Key");
        }

        ApiKeyAuthentication authentication = new ApiKeyAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateWithFirebaseToken(HttpServletRequest request) throws FirebaseAuthException {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearerToken == null || bearerToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Missing Authorization header");
        }

        if (!bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid Authorization header format. Expected: Bearer <token>");
        }

        String idToken = bearerToken.substring(7);
        FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

        // Validate audience
        String audience = (String) firebaseToken.getClaims().get("aud");
        if (!firebaseProjectId.equals(audience)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Token audience mismatch. Expected: " + firebaseProjectId + ", Got: " + audience);
        }

        // Validate issuer
        String expectedIssuer = "https://securetoken.google.com/" + firebaseProjectId;
        if (!expectedIssuer.equals(firebaseToken.getIssuer())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Token issuer mismatch. Expected: " + expectedIssuer + ", Got: " + firebaseToken.getIssuer());
        }

        // Validate subject (UID)
        if (firebaseToken.getUid() == null || firebaseToken.getUid().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user ID in token");
        }

        FirebaseAuthentication authentication = new FirebaseAuthentication(firebaseToken.getUid());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void logAuthenticationFailure(String method, String path, String reason) {
        logger.warn("Authentication failure - Method: {}, Path: {}, Reason: {}",
                    method, path, reason);
    }
}
