package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when access is forbidden (HTTP 403 Forbidden)
 */
@Getter
public class ForbiddenException extends RuntimeException {
    private final String errorCode;

    /**
     * Preferred constructor using ErrorCode enum
     */
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode.getCode();
    }

    /**
     * Constructor with ErrorCode enum and custom message
     */
    public ForbiddenException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode.getCode();
    }

    // Backward compatibility constructors
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
}
