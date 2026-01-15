package com.tateca.tatecabackend.exception.domain;

/**
 * Exception thrown when access is forbidden (HTTP 403 Forbidden)
 */
public class ForbiddenException extends RuntimeException {
    private final String errorCode;

    public ForbiddenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ForbiddenException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Backward compatibility constructors
    public ForbiddenException(String message) {
        super(message);
        this.errorCode = null;
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
