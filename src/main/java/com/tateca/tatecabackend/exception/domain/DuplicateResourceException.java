package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when attempting to create a resource that already exists.
 *
 * Enhancement: Now carries ErrorCode for i18n support.
 */
@Getter
public class DuplicateResourceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Constructor with ErrorCode for i18n support.
     *
     * @param errorCode Error code enum
     * @param messageArgs Message parameters (e.g., user email, group name)
     */
    public DuplicateResourceException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
