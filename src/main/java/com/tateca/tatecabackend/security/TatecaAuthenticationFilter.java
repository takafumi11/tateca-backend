package com.tateca.tatecabackend.security;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Spring Security filter that handles Firebase JWT authentication.
 * Replaces the old BearerTokenInterceptor with Spring Security integration.
 */
public class TatecaAuthenticationFilter extends OncePerRequestFilter {

    @Value("${firebase.project.id}")
    private String firebaseProjectId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (bearerToken == null || bearerToken.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing Authorization header");
            }

            // Firebase JWT authentication
            authenticateWithBearerToken(bearerToken);

            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid Firebase authentication token");
        } catch (ResponseStatusException e) {
            response.sendError(e.getStatusCode().value(), e.getReason());
        } finally {
            // Clear security context after request
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateWithBearerToken(String bearerToken) throws FirebaseAuthException {
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
}
