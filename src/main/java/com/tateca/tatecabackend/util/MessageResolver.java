package com.tateca.tatecabackend.util;

import com.tateca.tatecabackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility for resolving localized messages from MessageSource.
 *
 * Features:
 * - Automatic locale detection from LocaleContextHolder
 * - Type-safe error code resolution
 * - Fallback to English if message not found
 * - Support for message parameters
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    /**
     * Resolve message for error code with current locale.
     *
     * @param errorCode Error code enum
     * @param args Message parameters (e.g., user ID, group name)
     * @return Localized message
     */
    public String getMessage(ErrorCode errorCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(errorCode.getCode(), args, locale);
    }

    /**
     * Resolve message with explicit locale.
     *
     * @param errorCode Error code enum
     * @param locale Target locale
     * @param args Message parameters
     * @return Localized message
     */
    public String getMessage(ErrorCode errorCode, Locale locale, Object... args) {
        return messageSource.getMessage(errorCode.getCode(), args, locale);
    }

    /**
     * Resolve message by string key (for validation messages).
     *
     * @param key Message key
     * @param args Message parameters
     * @return Localized message
     */
    public String getMessage(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, key, locale);
    }
}
