package com.tateca.tatecabackend.exception.domain;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when a requested entity is not found.
 *
 * Enhancement: Now carries ErrorCode for i18n support.
 */
@Getter
public class EntityNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;

    /**
     * Constructor with ErrorCode for i18n support (recommended).
     *
     * @param errorCode Error code enum
     * @param messageArgs Message parameters (e.g., user ID, group ID)
     */
    public EntityNotFoundException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Legacy constructor for backward compatibility during migration.
     * @deprecated Use {@link #EntityNotFoundException(ErrorCode, Object...)} instead
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    public EntityNotFoundException(String message) {
        super(message);
        this.errorCode = null;
        this.messageArgs = null;
    }

    /**
     * Legacy constructor for backward compatibility during migration.
     * @deprecated Use {@link #EntityNotFoundException(ErrorCode, Object...)} instead
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.messageArgs = null;
    }
}
