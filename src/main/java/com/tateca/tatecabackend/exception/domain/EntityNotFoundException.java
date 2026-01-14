package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when a requested entity is not found.
 *
 * Uses ErrorCode for i18n support via MessageResolver.
 * Localized messages are defined in messages.properties and messages_en.properties.
 */
@Getter
public class EntityNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Constructor with ErrorCode for i18n support.
     *
     * @param errorCode Error code enum
     * @param messageArgs Message parameters (e.g., user ID, group ID)
     */
    public EntityNotFoundException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
