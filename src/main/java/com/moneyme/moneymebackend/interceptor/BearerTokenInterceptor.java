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

@Component
public class BearerTokenInterceptor implements HandlerInterceptor {
    public static final String UID_ATTRIBUTE = "uid";
    private static final String X_UID_HEADER = "x-uid";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String uid = request.getHeader(X_UID_HEADER);

        request.setAttribute(UID_ATTRIBUTE, uid);

        if (uid == null || uid.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing x-uid header");
        }

        try {
            FirebaseToken firebaseToken = FirebaseAuthHelper.verifyIdToken(bearerToken);
            if (!uid.equals(firebaseToken.getUid())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid x-uid header");
            }
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }

        return true;
    }

}