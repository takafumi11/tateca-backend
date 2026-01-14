package com.tateca.tatecabackend.exception;

import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for internationalization with HTTP status mapping.
 * Format: DOMAIN.ERROR_TYPE
 *
 * Benefits:
 * - Type-safe error code references
 * - Easy to search/grep across codebase
 * - Self-documenting error taxonomy
 * - Automatic HTTP status code mapping
 * - Client-side can handle specific errors (e.g., navigate to signup on USER.NOT_FOUND)
 */
public enum ErrorCode {
    // User Domain Errors (404 Not Found)
    USER_NOT_FOUND("USER.NOT_FOUND", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER.ALREADY_EXISTS", HttpStatus.CONFLICT),

    // Auth User Domain Errors
    AUTH_USER_NOT_FOUND("AUTH.USER_NOT_FOUND", HttpStatus.NOT_FOUND),
    AUTH_EMAIL_ALREADY_EXISTS("AUTH.EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT),

    // Group Domain Errors
    GROUP_NOT_FOUND("GROUP.NOT_FOUND", HttpStatus.NOT_FOUND),
    GROUP_MEMBER_NOT_FOUND("GROUP.MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND),
    GROUP_FULL("GROUP.FULL", HttpStatus.CONFLICT),
    GROUP_MAX_COUNT_EXCEEDED("GROUP.MAX_COUNT_EXCEEDED", HttpStatus.CONFLICT),
    GROUP_ALREADY_JOINED("GROUP.ALREADY_JOINED", HttpStatus.CONFLICT),
    GROUP_INVALID_TOKEN("GROUP.INVALID_TOKEN", HttpStatus.FORBIDDEN),

    // Transaction Domain Errors
    TRANSACTION_NOT_FOUND("TRANSACTION.NOT_FOUND", HttpStatus.NOT_FOUND),
    TRANSACTION_INVALID_AMOUNT("TRANSACTION.INVALID_AMOUNT", HttpStatus.BAD_REQUEST),
    TRANSACTION_INVALID_OBLIGATIONS("TRANSACTION.INVALID_OBLIGATIONS", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_CREATOR("TRANSACTION.NOT_CREATOR", HttpStatus.FORBIDDEN),

    // Exchange Rate Domain Errors
    EXCHANGE_RATE_NOT_FOUND("EXCHANGE_RATE.NOT_FOUND", HttpStatus.NOT_FOUND),

    // Validation Errors (400 Bad Request)
    VALIDATION_FAILED("VALIDATION.FAILED", HttpStatus.BAD_REQUEST),
    VALIDATION_FIELD_REQUIRED("VALIDATION.FIELD_REQUIRED", HttpStatus.BAD_REQUEST),
    VALIDATION_FIELD_INVALID_FORMAT("VALIDATION.FIELD_INVALID_FORMAT", HttpStatus.BAD_REQUEST),
    VALIDATION_FIELD_LENGTH("VALIDATION.FIELD_LENGTH", HttpStatus.BAD_REQUEST),

    // Database Errors (500/409)
    DATABASE_ERROR("DATABASE.ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_CONSTRAINT_VIOLATION("DATABASE.CONSTRAINT_VIOLATION", HttpStatus.CONFLICT),

    // Request Errors (400/415)
    REQUEST_INVALID_FORMAT("REQUEST.INVALID_FORMAT", HttpStatus.BAD_REQUEST),
    REQUEST_INVALID_PARAMETER("REQUEST.INVALID_PARAMETER", HttpStatus.BAD_REQUEST),
    REQUEST_UNSUPPORTED_MEDIA_TYPE("REQUEST.UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // System Errors (500)
    SYSTEM_INTERNAL_ERROR("SYSTEM.INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    SYSTEM_UNEXPECTED_ERROR("SYSTEM.UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
