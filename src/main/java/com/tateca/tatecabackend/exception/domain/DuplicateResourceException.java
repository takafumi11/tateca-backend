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
     * Constructor with ErrorCode for i18n support (recommended).
     *
     * @param errorCode Error code enum
     * @param messageArgs Message parameters (e.g., user email, group name)
     */
    public DuplicateResourceException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Legacy constructor for backward compatibility during migration.
     * @deprecated Use {@link #DuplicateResourceException(ErrorCode, Object...)} instead
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    public DuplicateResourceException(String message) {
        super(message);
        this.errorCode = null;
        this.messageArgs = null;
    }

    /**
     * Legacy constructor for backward compatibility during migration.
     * @deprecated Use {@link #DuplicateResourceException(ErrorCode, Object...)} instead
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.messageArgs = null;
    }
}
