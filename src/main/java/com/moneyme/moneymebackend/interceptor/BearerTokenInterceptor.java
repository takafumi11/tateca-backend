package com.moneyme.moneymebackend.interceptor;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseToken;
import com.moneyme.moneymebackend.exception.CustomResponseStatusException;
import com.moneyme.moneymebackend.service.util.FirebaseAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZonedDateTime;

@Component
public class BearerTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();
        String apiName = httpMethod + " " + uri;

        try {
            FirebaseToken firebaseToken = FirebaseAuthHelper.verifyIdToken(bearerToken, request.getRequestURI());
            request.setAttribute("uid", firebaseToken.getUid());
            return true;
        } catch (Exception e) {
            throw new CustomResponseStatusException(apiName, "unknown", "Invalid Bearer Token", HttpStatus.UNAUTHORIZED);
        }
    }
}