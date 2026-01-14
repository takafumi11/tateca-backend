package com.tateca.tatecabackend.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Internationalization configuration for error messages and validation.
 *
 * Strategy:
 * - Accept-Language header for locale detection
 * - Japanese as default locale
 * - English as fallback
 * - UTF-8 encoding for Japanese characters
 */
@Configuration
public class MessageSourceConfig {

    /**
     * Message source for error messages and general i18n.
     * Loads messages from messages_*.properties files.
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.JAPAN);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true); // Return code if message not found (dev safety)
        messageSource.setCacheSeconds(3600); // Cache for 1 hour in production
        return messageSource;
    }

    /**
     * Locale resolver using Accept-Language header.
     * Falls back to Japanese if header is missing or invalid.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.JAPAN);
        resolver.setSupportedLocales(List.of(Locale.JAPAN, Locale.ENGLISH));
        return resolver;
    }

    /**
     * Validator configured to use MessageSource for validation messages.
     * Enables i18n for @NotBlank, @Size, etc. annotations.
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
