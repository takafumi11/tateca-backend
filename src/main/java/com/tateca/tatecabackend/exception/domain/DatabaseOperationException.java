package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when a database operation fails.
 * This exception wraps infrastructure-layer exceptions (DataAccessException)
 * to maintain clean architecture boundaries.
 */
@Getter
public class DatabaseOperationException extends RuntimeException {

    private final String errorCode;

    /**
     * Preferred constructor using ErrorCode enum
     */
    public DatabaseOperationException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode.getCode();
    }

    /**
     * Constructor with ErrorCode enum and cause
     */
    public DatabaseOperationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode.getCode();
    }

    /**
     * Constructor with ErrorCode enum, custom message, and cause
     */
    public DatabaseOperationException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode.getCode();
    }

    // Backward compatibility constructors
    public DatabaseOperationException(String message) {
        super(message);
        this.errorCode = "DATABASE_OPERATION_ERROR";
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DATABASE_OPERATION_ERROR";
    }

    public DatabaseOperationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}
