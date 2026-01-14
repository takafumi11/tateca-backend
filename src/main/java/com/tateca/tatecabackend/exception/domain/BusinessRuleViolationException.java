package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when a business rule is violated.
 * This is a generic exception for business logic violations that don't fit
 * into more specific categories like EntityNotFoundException or DuplicateResourceException.
 *
 * Examples:
 * - Invalid join token
 * - Maximum group count exceeded
 * - User already joined a group
 * - Business constraints violations
 */
@Getter
public class BusinessRuleViolationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Constructor with ErrorCode for i18n support (recommended).
     *
     * @param errorCode Error code enum
     * @param messageArgs Message parameters (e.g., group ID, token)
     */
    public BusinessRuleViolationException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Legacy constructor for backward compatibility during migration.
     * @deprecated Use {@link #BusinessRuleViolationException(ErrorCode, Object...)} instead
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    public BusinessRuleViolationException(String message) {
        super(message);
        this.errorCode = null;
        this.messageArgs = null;
    }

    /**
     * Legacy constructor for backward compatibility during migration.
     * @deprecated Use {@link #BusinessRuleViolationException(ErrorCode, Object...)} instead
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.messageArgs = null;
    }
}
