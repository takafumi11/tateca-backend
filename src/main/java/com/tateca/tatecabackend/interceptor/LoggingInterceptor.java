package com.tateca.tatecabackend.interceptor;

import com.tateca.tatecabackend.security.FirebaseAuthentication;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.UUID;

import static com.tateca.tatecabackend.constants.AttributeConstants.REQUEST_ID_ATTRIBUTE;
import static com.tateca.tatecabackend.constants.AttributeConstants.REQUEST_TIME_ATTRIBUTE;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Instant requestTime = Instant.now();

        // Store in request attributes for afterCompletion
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(REQUEST_TIME_ATTRIBUTE, requestTime);

        // Add to MDC for correlation across logs
        MDC.put("requestId", requestId);

        // Extract and add userId to MDC
        String uid = getUidFromSecurityContext();
        if (uid != null) {
            MDC.put("userId", PiiMaskingUtil.hashUserId(uid));
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);
        Instant requestTime = (Instant) request.getAttribute(REQUEST_TIME_ATTRIBUTE);
        String uid = getUidFromSecurityContext();

        Instant responseTime = Instant.now();
        long processingTimeMs = responseTime.toEpochMilli() - requestTime.toEpochMilli();
        int status = response.getStatus();

        // Add request details to MDC for structured logging
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        // Log request
        logger.info("HTTP Request");

        // Add response metrics to MDC for structured logging
        MDC.put("status", String.valueOf(status));
        MDC.put("latencyMs", String.valueOf(processingTimeMs));

        // Log response
        logger.info("HTTP Response");

        // Clear MDC to prevent memory leaks in thread pools
        MDC.clear();
    }

    private String getUidFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof FirebaseAuthentication) {
            return ((FirebaseAuthentication) authentication).getUid();
        }

        return null;
    }
}