package com.moneyme.moneymebackend.annotation;

import com.google.common.net.HttpHeaders;
import com.moneyme.moneymebackend.service.util.FirebaseAuthHelper;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class BearerTokenArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(ValidBearerToken.class) != null;
    }

    @Override
    public String resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        String bearerToken = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        FirebaseAuthHelper.verifyIdToken(bearerToken);
        return bearerToken;
    }
}