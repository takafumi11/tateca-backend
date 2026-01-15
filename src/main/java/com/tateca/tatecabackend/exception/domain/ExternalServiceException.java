package com.tateca.tatecabackend.exception.domain;

/**
 * Exception thrown when external service calls fail (HTTP 500 Internal Server Error or 502/503)
 */
public class ExternalServiceException extends RuntimeException {
    private final String errorCode;

    public ExternalServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ExternalServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Backward compatibility constructors
    public ExternalServiceException(String message) {
        super(message);
        this.errorCode = null;
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
