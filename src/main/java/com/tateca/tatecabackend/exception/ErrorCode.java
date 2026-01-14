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
 * - Default English messages for logging and testing
 * - Client-side can handle specific errors (e.g., navigate to signup on USER.NOT_FOUND)
 */
public enum ErrorCode {
    // User Domain Errors (404 Not Found)
    USER_NOT_FOUND("USER.NOT_FOUND", HttpStatus.NOT_FOUND, "User not found: %s"),
    USER_ALREADY_EXISTS("USER.ALREADY_EXISTS", HttpStatus.CONFLICT, "User already exists: %s"),

    // Auth User Domain Errors
    AUTH_USER_NOT_FOUND("AUTH.USER_NOT_FOUND", HttpStatus.NOT_FOUND, "AuthUser not found: %s"),
    AUTH_EMAIL_ALREADY_EXISTS("AUTH.EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "Email already exists: %s"),

    // Group Domain Errors
    GROUP_NOT_FOUND("GROUP.NOT_FOUND", HttpStatus.NOT_FOUND, "Group not found: %s"),
    GROUP_MEMBER_NOT_FOUND("GROUP.MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "Group member not found"),
    GROUP_FULL("GROUP.FULL", HttpStatus.CONFLICT, "Group is full: %s"),
    GROUP_MAX_COUNT_EXCEEDED("GROUP.MAX_COUNT_EXCEEDED", HttpStatus.CONFLICT, "User has reached the maximum number of groups: %s"),
    GROUP_ALREADY_JOINED("GROUP.ALREADY_JOINED", HttpStatus.CONFLICT, "User has already joined the group"),
    GROUP_INVALID_TOKEN("GROUP.INVALID_TOKEN", HttpStatus.FORBIDDEN, "Invalid join token"),

    // Transaction Domain Errors
    TRANSACTION_NOT_FOUND("TRANSACTION.NOT_FOUND", HttpStatus.NOT_FOUND, "Transaction not found: %s"),
    TRANSACTION_INVALID_AMOUNT("TRANSACTION.INVALID_AMOUNT", HttpStatus.BAD_REQUEST, "Invalid transaction amount"),
    TRANSACTION_INVALID_OBLIGATIONS("TRANSACTION.INVALID_OBLIGATIONS", HttpStatus.BAD_REQUEST, "Invalid obligations"),
    TRANSACTION_NOT_CREATOR("TRANSACTION.NOT_CREATOR", HttpStatus.FORBIDDEN, "Only the creator can delete this transaction"),

    // Exchange Rate Domain Errors
    EXCHANGE_RATE_NOT_FOUND("EXCHANGE_RATE.NOT_FOUND", HttpStatus.NOT_FOUND, "Exchange rate not found: %s"),

    // Validation Errors (400 Bad Request)
    VALIDATION_FAILED("VALIDATION.FAILED", HttpStatus.BAD_REQUEST, "Validation failed"),
    VALIDATION_FIELD_REQUIRED("VALIDATION.FIELD_REQUIRED", HttpStatus.BAD_REQUEST, "Field is required"),
    VALIDATION_FIELD_INVALID_FORMAT("VALIDATION.FIELD_INVALID_FORMAT", HttpStatus.BAD_REQUEST, "Invalid field format"),
    VALIDATION_FIELD_LENGTH("VALIDATION.FIELD_LENGTH", HttpStatus.BAD_REQUEST, "Invalid field length"),

    // Database Errors (500/409)
    DATABASE_ERROR("DATABASE.ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred"),
    DATABASE_CONSTRAINT_VIOLATION("DATABASE.CONSTRAINT_VIOLATION", HttpStatus.CONFLICT, "Database constraint violation"),

    // Request Errors (400/415)
    REQUEST_INVALID_FORMAT("REQUEST.INVALID_FORMAT", HttpStatus.BAD_REQUEST, "Invalid request format"),
    REQUEST_INVALID_PARAMETER("REQUEST.INVALID_PARAMETER", HttpStatus.BAD_REQUEST, "Invalid parameter"),
    REQUEST_UNSUPPORTED_MEDIA_TYPE("REQUEST.UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),

    // System Errors (500)
    SYSTEM_INTERNAL_ERROR("SYSTEM.INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    SYSTEM_UNEXPECTED_ERROR("SYSTEM.UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
