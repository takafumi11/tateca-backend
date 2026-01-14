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
 *
 * Uses ErrorCode for i18n support via MessageResolver.
 * Localized messages are defined in messages.properties and messages_en.properties.
 */
@Getter
public class BusinessRuleViolationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Constructor with ErrorCode for i18n support.
     *
     * @param errorCode Error code enum
     * @param messageArgs Message parameters (e.g., group ID, token)
     */
    public BusinessRuleViolationException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
