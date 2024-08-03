package com.moneyme.moneymebackend.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class RequestTimeArgumentResolver implements HandlerMethodArgumentResolver {
    static final String REQUEST_TIME_ATTRIBUTE = "requestTime";
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(RequestTime.class) != null;
    }

    @Override
    public ZonedDateTime resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        ZonedDateTime requestTime = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));
        webRequest.setAttribute(REQUEST_TIME_ATTRIBUTE, requestTime, NativeWebRequest.SCOPE_REQUEST);
        return requestTime;
    }
}
