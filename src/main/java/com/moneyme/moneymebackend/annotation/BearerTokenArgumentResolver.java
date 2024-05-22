package com.moneyme.moneymebackend.annotation;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuthException;
import com.moneyme.moneymebackend.service.util.FirebaseAuthHelper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BearerTokenArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(ValidBearerToken.class) != null;
    }

    @Override
    public String resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) throws Exception {
        String bearerToken = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing Authorization header");
        }
        try {
            FirebaseAuthHelper.verifyIdToken(bearerToken);
            return bearerToken;
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Bearer Token");
        }
    }
}