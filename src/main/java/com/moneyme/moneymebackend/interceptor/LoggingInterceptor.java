package com.moneyme.moneymebackend.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_TIME_ATTRIBUTE = "requestTime";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.of("Asia/Tokyo"));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Instant requestTime = Instant.now();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(REQUEST_TIME_ATTRIBUTE, requestTime);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        String requestBody = getRequestBody(requestWrapper);

        logger.info("Request: [{}] {} {} - RequestTime: {} - Body: {}",
                requestId, request.getMethod(), request.getRequestURI(), formatter.format(requestTime), requestBody);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);
        Instant requestTime = (Instant) request.getAttribute(REQUEST_TIME_ATTRIBUTE);
        Instant responseTime = Instant.now();
        long processingTimeMs = responseTime.toEpochMilli() - requestTime.toEpochMilli();

        ContentCachingResponseWrapper responseWrapper = getWrapper(response, ContentCachingResponseWrapper.class);
        String responseBody = responseWrapper != null ? getResponseBody(responseWrapper) : "N/A";

        logger.info("Response: [{}] {} - ResponseTime: {}ms - Body: {}",
                requestId, response.getStatus(), processingTimeMs, responseBody);

        if (responseWrapper != null) {
            responseWrapper.copyBodyToResponse();
        }
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

    private String getResponseBody(ContentCachingResponseWrapper response) throws IOException {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }
}