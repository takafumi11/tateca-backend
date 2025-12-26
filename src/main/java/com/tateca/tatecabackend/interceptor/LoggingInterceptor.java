package com.tateca.tatecabackend.interceptor;

import com.tateca.tatecabackend.security.FirebaseAuthentication;
import com.tateca.tatecabackend.security.LambdaAuthentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static com.tateca.tatecabackend.constants.AttributeConstants.REQUEST_ID_ATTRIBUTE;
import static com.tateca.tatecabackend.constants.AttributeConstants.REQUEST_TIME_ATTRIBUTE;
import static com.tateca.tatecabackend.service.util.TimeHelper.TOKYO_ZONE_ID;
import static com.tateca.tatecabackend.service.util.TimeHelper.DATE_TIME_FORMATTER;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Instant requestTime = Instant.now();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(REQUEST_TIME_ATTRIBUTE, requestTime);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);
        Instant requestTime = (Instant) request.getAttribute(REQUEST_TIME_ATTRIBUTE);
        String uid = getUidFromSecurityContext();

        uid = (uid != null) ? uid : "unknown";

        Instant responseTime = Instant.now();
        long processingTimeMs = responseTime.toEpochMilli() - requestTime.toEpochMilli();

        ContentCachingRequestWrapper requestWrapper = getWrapper(request, ContentCachingRequestWrapper.class);

        String requestBody = requestWrapper != null ? getRequestBody(requestWrapper) : "N/A";

        logger.info("Request: Method: {} - Path: {} - UID: {} - Body: {} - RequestTime: {} - RequestId: [{}]", request.getMethod(), request.getRequestURI(), uid, requestBody, DATE_TIME_FORMATTER.withZone(TOKYO_ZONE_ID).format(requestTime), requestId);
        logger.info("Response: Status: {} - ProcessingTime: {}ms - RequestId: [{}]", response.getStatus(), processingTimeMs, requestId);
    }

    private String getUidFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof FirebaseAuthentication) {
            return ((FirebaseAuthentication) authentication).getUid();
        } else if (authentication instanceof LambdaAuthentication) {
            return ((LambdaAuthentication) authentication).getUid();
        }

        return null;
    }

    private <T> T getWrapper(Object obj, Class<T> wrapper) {
        if (wrapper.isInstance(obj)) {
            return (T) obj;
        }
        return null;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }

    // TODO: Want to use
    private String getResponseBody(ContentCachingResponseWrapper response) throws IOException {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }
}