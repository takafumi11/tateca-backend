package com.tateca.tatecabackend.exception.domain;

public class EntityNotFoundException extends RuntimeException {
    private final String errorCode;

    public EntityNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public EntityNotFoundException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Backward compatibility constructors
    public EntityNotFoundException(String message) {
        super(message);
        this.errorCode = null;
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
