package com.tateca.tatecabackend.exception.domain;

/**
 * Exception thrown when external service calls fail (HTTP 500 Internal Server Error or 502/503)
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
