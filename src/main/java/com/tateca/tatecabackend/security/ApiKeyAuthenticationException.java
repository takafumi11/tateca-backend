package com.tateca.tatecabackend.security;

/**
 * Exception thrown when API Key authentication fails.
 * Used by TatecaAuthenticationFilter for internal endpoint authentication errors.
 */
public class ApiKeyAuthenticationException extends RuntimeException {
    public ApiKeyAuthenticationException(String message) {
        super(message);
    }
}
