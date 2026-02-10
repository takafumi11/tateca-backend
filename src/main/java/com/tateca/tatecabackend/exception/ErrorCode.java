package com.tateca.tatecabackend.exception;

/**
 * Centralized error code definitions for API responses and logging.
 *
 * <p>Error codes follow the format: ENTITY.ERROR_TYPE
 * Examples: USER.NOT_FOUND, TRANSACTION.INVALID_AMOUNT
 *
 * <p>Usage:
 * <pre>
 * throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
 * </pre>
 *
 * <p>Frontend Integration:
 * Error codes are returned in API responses for client-side handling:
 * <pre>
 * {
 *   "errorCode": "USER.NOT_FOUND",
 *   "message": "User not found"
 * }
 * </pre>
 */
public enum ErrorCode {

    // ==================== User Errors ====================
    /**
     * User entity not found in database
     */
    USER_NOT_FOUND("USER.NOT_FOUND", "User not found"),

    // ==================== Database Errors ====================
    /**
     * Generic database operation failure
     */
    DATABASE_OPERATION_ERROR("DATABASE.OPERATION_ERROR", "Database operation failed");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Returns the error code string (e.g., "USER.NOT_FOUND")
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the default English message for developers and logs
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return code;
    }
}
