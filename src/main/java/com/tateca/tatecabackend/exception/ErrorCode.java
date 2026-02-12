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

    /**
     * User is not in the specified group
     */
    USER_NOT_IN_GROUP("USER.NOT_IN_GROUP", "User is not in this group"),

    /**
     * User is not a member of the group (authorization)
     */
    USER_NOT_GROUP_MEMBER("USER.NOT_GROUP_MEMBER", "Only group members can add members"),

    /**
     * User has exceeded maximum group count limit
     */
    USER_MAX_GROUP_COUNT_EXCEEDED("USER.MAX_GROUP_COUNT_EXCEEDED", "User can't join more than 10 groups"),

    // ==================== Auth User Errors ====================
    /**
     * Auth user entity not found in database
     */
    AUTH_USER_NOT_FOUND("AUTH_USER.NOT_FOUND", "Auth user not found"),

    /**
     * Email address already exists in the system
     */
    AUTH_USER_EMAIL_DUPLICATE("AUTH_USER.EMAIL_DUPLICATE", "Email already exists"),

    // ==================== Group Errors ====================
    /**
     * Group entity not found in database
     */
    GROUP_NOT_FOUND("GROUP.NOT_FOUND", "Group not found"),

    /**
     * User has already joined the specified group
     */
    GROUP_ALREADY_JOINED("GROUP.ALREADY_JOINED", "You have already joined this group"),

    /**
     * Group has reached maximum size limit
     */
    GROUP_MAX_SIZE_REACHED("GROUP.MAX_SIZE_REACHED", "Group has reached maximum size of 10 members"),

    /**
     * Invalid or expired join token provided
     */
    GROUP_INVALID_JOIN_TOKEN("GROUP.INVALID_JOIN_TOKEN", "Invalid or expired join token");

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
