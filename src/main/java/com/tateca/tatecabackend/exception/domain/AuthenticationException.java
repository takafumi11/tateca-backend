package com.tateca.tatecabackend.exception.domain;

/**
 * Exception thrown when authentication fails (HTTP 401 Unauthorized)
 */
public class AuthenticationException extends RuntimeException {
    private final String errorCode;

    public AuthenticationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthenticationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Backward compatibility constructors
    public AuthenticationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
