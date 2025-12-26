package com.tateca.tatecabackend.annotation;

import com.tateca.tatecabackend.security.ApiKeyAuthentication;
import com.tateca.tatecabackend.security.FirebaseAuthentication;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves @UId annotation by extracting UID from Spring Security context.
 * Compatible with FirebaseAuthentication and ApiKeyAuthentication.
 */
@Component
public class UIdArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(UId.class) != null;
    }

    @Override
    public String resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest,
                                 org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof FirebaseAuthentication) {
            return ((FirebaseAuthentication) authentication).getUid();
        }

        if (authentication instanceof ApiKeyAuthentication) {
            return ((ApiKeyAuthentication) authentication).getUid();
        }

        // Fallback to principal name (should not happen with proper security config)
        return authentication != null ? authentication.getName() : null;
    }
}