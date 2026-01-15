package com.tateca.tatecabackend.exception.domain;

/**
 * Exception thrown when business rules are violated (HTTP 409 Conflict or 400 Bad Request)
 */
public class BusinessRuleViolationException extends RuntimeException {
    private final String errorCode;

    public BusinessRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessRuleViolationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Backward compatibility constructors
    public BusinessRuleViolationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
