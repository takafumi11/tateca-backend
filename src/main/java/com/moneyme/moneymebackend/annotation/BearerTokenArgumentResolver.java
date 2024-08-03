package com.moneyme.moneymebackend.annotation;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseToken;
import com.moneyme.moneymebackend.service.util.FirebaseAuthHelper;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class BearerTokenArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(ValidBearerToken.class) != null;
    }

    @Override
    public BearerTokenWithRequestTime resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        ZonedDateTime requestTime = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));
        String bearerToken = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        FirebaseToken firebaseToken = FirebaseAuthHelper.verifyIdToken(bearerToken, parameter.getMethod().getName(), requestTime);
        return new BearerTokenWithRequestTime(firebaseToken.getUid(), requestTime);
    }
}