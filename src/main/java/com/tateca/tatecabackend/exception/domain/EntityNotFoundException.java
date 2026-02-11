package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {
    private final String errorCode;

    /**
     * Preferred constructor using ErrorCode enum
     */
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode.getCode();
    }

    /**
     * Constructor with ErrorCode enum and custom message
     */
    public EntityNotFoundException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode.getCode();
    }

    // Backward compatibility constructors
    public EntityNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public EntityNotFoundException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.errorCode = null;
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

}
