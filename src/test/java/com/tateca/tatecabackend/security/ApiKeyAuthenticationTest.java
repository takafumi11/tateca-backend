package com.tateca.tatecabackend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiKeyAuthentication Tests")
class ApiKeyAuthenticationTest {

    @Test
    @DisplayName("Should return system UID")
    void shouldReturnSystemUid() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        String uid = authentication.getUid();

        // Then
        assertThat(uid).isEqualTo("system-internal");
    }

    @Test
    @DisplayName("Should return system UID as name")
    void shouldReturnSystemUidAsName() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        String name = authentication.getName();

        // Then
        assertThat(name).isEqualTo("system-internal");
    }

    @Test
    @DisplayName("Should return system UID as principal")
    void shouldReturnSystemUidAsPrincipal() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        Object principal = authentication.getPrincipal();

        // Then
        assertThat(principal).isEqualTo("system-internal");
    }

    @Test
    @DisplayName("Should return null for credentials")
    void shouldReturnNullForCredentials() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        Object credentials = authentication.getCredentials();

        // Then
        assertThat(credentials).isNull();
    }

    @Test
    @DisplayName("Should return ROLE_SYSTEM authority")
    void shouldReturnRoleSystemAuthority() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Then
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_SYSTEM");
    }

    @Test
    @DisplayName("Should be authenticated")
    void shouldBeAuthenticated() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        boolean isAuthenticated = authentication.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when trying to change authentication state")
    void shouldThrowExceptionWhenChangingAuthenticationState() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When/Then
        assertThatThrownBy(() -> authentication.setAuthenticated(false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot change authentication state");
    }

    @Test
    @DisplayName("Should return null for details")
    void shouldReturnNullForDetails() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();

        // When
        Object details = authentication.getDetails();

        // Then
        assertThat(details).isNull();
    }
}
