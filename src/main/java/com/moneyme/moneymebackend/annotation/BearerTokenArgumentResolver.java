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

import static com.moneyme.moneymebackend.annotation.RequestTimeArgumentResolver.REQUEST_TIME_ATTRIBUTE;


@Component
public class BearerTokenArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(ValidBearerToken.class) != null;
    }

    @Override
    public String resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        ZonedDateTime requestTime = (ZonedDateTime) webRequest.getAttribute(REQUEST_TIME_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        String bearerToken = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        FirebaseToken firebaseToken = FirebaseAuthHelper.verifyIdToken(requestTime, bearerToken, parameter.getMethod().getName());
        return firebaseToken.getUid();
    }
}