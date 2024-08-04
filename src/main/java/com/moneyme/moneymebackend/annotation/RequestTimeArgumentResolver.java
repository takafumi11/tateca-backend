package com.moneyme.moneymebackend.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.ZonedDateTime;

import static com.moneyme.moneymebackend.constants.AttributeConstants.REQUEST_TIME_ATTRIBUTE;

@Component
public class RequestTimeArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(RequestTime.class) != null;
    }

    @Override
    public ZonedDateTime resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        return (ZonedDateTime) webRequest.getAttribute(REQUEST_TIME_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
    }
}
