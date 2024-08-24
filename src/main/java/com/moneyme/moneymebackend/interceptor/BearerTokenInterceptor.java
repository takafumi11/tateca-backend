package com.moneyme.moneymebackend.interceptor;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.moneyme.moneymebackend.service.util.FirebaseAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.moneyme.moneymebackend.constants.ApiConstants.X_UID_HEADER;
import static com.moneyme.moneymebackend.constants.AttributeConstants.UID_ATTRIBUTE;

@Component
public class BearerTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            FirebaseToken firebaseToken = FirebaseAuthHelper.verifyIdToken(bearerToken);
            request.setAttribute(UID_ATTRIBUTE, firebaseToken.getUid());
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }

        return true;
    }

}