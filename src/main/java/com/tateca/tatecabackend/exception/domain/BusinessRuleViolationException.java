package com.tateca.tatecabackend.exception.domain;

/**
 * Exception thrown when business rules are violated (HTTP 409 Conflict or 400 Bad Request)
 */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
