package com.tateca.tatecabackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.exception.domain.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security filter that handles multiple authentication methods:
 * - API Key authentication for internal endpoints (/internal/**)
 * - Firebase JWT authentication for user endpoints
 * - Dev mode: x-uid header bypass (dev profile only)
 */
@RequiredArgsConstructor
public class TatecaAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TatecaAuthenticationFilter.class);
    private static final String X_API_KEY_HEADER = "X-API-Key";

    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${firebase.project.id}")
    private String firebaseProjectId;

    @Value("${lambda.api.key}")
    private String lambdaApiKey;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/error",
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
            writeJsonErrorResponse(response, HttpStatus.UNAUTHORIZED, "AUTH.INVALID_TOKEN", "Invalid API Key", path);
        } catch (FirebaseAuthException e) {
            logAuthenticationFailure("Firebase", path, e.getMessage());
            writeJsonErrorResponse(response, HttpStatus.UNAUTHORIZED, "AUTH.INVALID_TOKEN", "Invalid Firebase authentication token", path);
        } catch (AuthenticationException e) {
            logAuthenticationFailure("Authentication", path, e.getMessage());
            String errorCode = e.getErrorCode() != null ? e.getErrorCode() : "AUTH.INVALID_TOKEN";
            writeJsonErrorResponse(response, HttpStatus.UNAUTHORIZED, errorCode, e.getMessage(), path);
        } catch (IllegalArgumentException e) {
            logAuthenticationFailure("Bad Request", path, e.getMessage());
            response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
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
        // Dev mode: Allow x-uid header bypass
        if (isDevProfile()) {
            String xUid = request.getHeader(ApiConstants.X_UID_HEADER);
            if (xUid != null && !xUid.isEmpty()) {
                logger.debug("Dev mode: Authenticating with x-uid header: {}", xUid);
                FirebaseAuthentication authentication = new FirebaseAuthentication(xUid);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return;
            }
        }

        // Production mode: Verify Firebase ID token
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearerToken == null || bearerToken.isEmpty()) {
            logger.warn("Missing Authorization header for request: {} {}", request.getMethod(), request.getRequestURI());
            throw new AuthenticationException("AUTH.MISSING_CREDENTIALS", "Authentication required");
        }

        if (!bearerToken.startsWith("Bearer ")) {
            logger.warn("Invalid Authorization header format for request: {} {}", request.getMethod(), request.getRequestURI());
            throw new AuthenticationException("AUTH.INVALID_FORMAT", "Invalid authentication credentials");
        }

        String idToken = bearerToken.substring(7);
        // SECURITY: Enable token revocation check to immediately invalidate logged-out users
        // This prevents compromised tokens from being used after logout/password change
        // Performance impact: +10-50ms per request (Firebase database lookup)
        FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(idToken, true);

        // Validate audience
        String audience = (String) firebaseToken.getClaims().get("aud");
        if (!firebaseProjectId.equals(audience)) {
            logger.warn("Token audience mismatch: expected={}, got={}", firebaseProjectId, audience);
            throw new AuthenticationException("AUTH.INVALID_TOKEN", "Invalid authentication token");
        }

        // Validate issuer
        String expectedIssuer = "https://securetoken.google.com/" + firebaseProjectId;
        if (!expectedIssuer.equals(firebaseToken.getIssuer())) {
            logger.warn("Token issuer mismatch: expected={}, got={}", expectedIssuer, firebaseToken.getIssuer());
            throw new AuthenticationException("AUTH.INVALID_TOKEN", "Invalid authentication token");
        }

        // Validate subject (UID)
        if (firebaseToken.getUid() == null || firebaseToken.getUid().isEmpty()) {
            logger.warn("Missing user ID in token");
            throw new AuthenticationException("AUTH.MISSING_USER_ID", "Missing user ID in token");
        }

        FirebaseAuthentication authentication = new FirebaseAuthentication(firebaseToken.getUid());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private void writeJsonErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message, String path) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .errorCode(errorCode)
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    private void logAuthenticationFailure(String method, String path, String reason) {
        logger.warn("Authentication failure - Method: {}, Path: {}, Reason: {}",
                    method, path, reason);
    }
}
