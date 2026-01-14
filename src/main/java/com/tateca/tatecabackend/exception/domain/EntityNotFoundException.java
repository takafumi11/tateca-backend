package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when a requested entity is not found.
 *
 * Uses ErrorCode for both i18n support (via MessageResolver in API responses)
 * and default English messages (for logging and testing).
 */
@Getter
public class EntityNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Constructor with ErrorCode for i18n support.
     *
     * @param errorCode Error code enum with default message template
     * @param messageArgs Message parameters (e.g., user ID, group ID)
     */
    public EntityNotFoundException(ErrorCode errorCode, Object... messageArgs) {
        super(formatMessage(errorCode, messageArgs));
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Format default English message for logging and testing.
     * API responses use localized messages from MessageResolver.
     */
    private static String formatMessage(ErrorCode errorCode, Object... args) {
        String template = errorCode.getDefaultMessage();
        if (args != null && args.length > 0) {
            return String.format(template, args);
        }
        return template;
    }
}
