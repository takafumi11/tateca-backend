package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when business rules are violated (HTTP 409 Conflict or 400 Bad Request)
 */
@Getter
public class BusinessRuleViolationException extends RuntimeException {
    private final String errorCode;

    /**
     * Preferred constructor using ErrorCode enum
     */
    public BusinessRuleViolationException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode.getCode();
    }

    /**
     * Constructor with ErrorCode enum and custom message
     */
    public BusinessRuleViolationException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode.getCode();
    }

    // Backward compatibility constructors
    public BusinessRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessRuleViolationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BusinessRuleViolationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

}
