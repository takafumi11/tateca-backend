package com.tateca.tatecabackend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiKeyAuthenticationException Tests")
class ApiKeyAuthenticationExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Invalid API Key";

        // When
        ApiKeyAuthenticationException exception = new ApiKeyAuthenticationException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
        // Given
        String message = "Test exception";

        // When/Then
        try {
            throw new ApiKeyAuthenticationException(message);
        } catch (ApiKeyAuthenticationException e) {
            assertThat(e.getMessage()).isEqualTo(message);
        }
    }
}
