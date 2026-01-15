package com.tateca.tatecabackend.exception.domain;

public class DuplicateResourceException extends RuntimeException {
    private final String errorCode;

    public DuplicateResourceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DuplicateResourceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Backward compatibility constructors
    public DuplicateResourceException(String message) {
        super(message);
        this.errorCode = null;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
