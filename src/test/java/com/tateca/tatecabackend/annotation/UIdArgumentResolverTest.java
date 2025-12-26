package com.tateca.tatecabackend.annotation;

import com.tateca.tatecabackend.security.ApiKeyAuthentication;
import com.tateca.tatecabackend.security.FirebaseAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("UIdArgumentResolver Tests")
class UIdArgumentResolverTest {

    private UIdArgumentResolver resolver;
    private MethodParameter parameter;
    private ModelAndViewContainer mavContainer;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        resolver = new UIdArgumentResolver();
        parameter = mock(MethodParameter.class);
        mavContainer = mock(ModelAndViewContainer.class);
        webRequest = mock(NativeWebRequest.class);

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should resolve UID from FirebaseAuthentication")
    void shouldResolveUidFromFirebaseAuthentication() {
        // Given
        String expectedUid = "firebase-user-123";
        FirebaseAuthentication authentication = new FirebaseAuthentication(expectedUid);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        String uid = resolver.resolveArgument(parameter, mavContainer, webRequest, null);

        // Then
        assertThat(uid).isEqualTo(expectedUid);
    }

    @Test
    @DisplayName("Should resolve UID from ApiKeyAuthentication")
    void shouldResolveUidFromApiKeyAuthentication() {
        // Given
        ApiKeyAuthentication authentication = new ApiKeyAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        String uid = resolver.resolveArgument(parameter, mavContainer, webRequest, null);

        // Then
        assertThat(uid).isEqualTo("system-internal");
    }

    @Test
    @DisplayName("Should return null when authentication is null")
    void shouldReturnNullWhenAuthenticationIsNull() {
        // Given: No authentication set (SecurityContext is cleared in setUp)

        // When
        String uid = resolver.resolveArgument(parameter, mavContainer, webRequest, null);

        // Then
        assertThat(uid).isNull();
    }
}
