package com.tateca.tatecabackend.interceptor;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.tateca.tatecabackend.constants.AttributeConstants.UID_ATTRIBUTE;

@Component
public class BearerTokenInterceptor implements HandlerInterceptor {

    @Value("${lambda.api.key}")
    private String lambdaApiKey;
    
    private static final String X_API_KEY_HEADER = "X-API-KEY";
    private static final String LAMBDA_UID = "lambda-system";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String apiKey = request.getHeader(X_API_KEY_HEADER);
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            String uid;
            
            // 認証方法の決定：X-API-KEYの存在をチェック
            if (apiKey != null && !apiKey.isEmpty()) {
                // Lambda用：X-API-KEY認証
                uid = authenticateWithApiKey(apiKey);
            } else if (bearerToken != null && !bearerToken.isEmpty()) {
                // iOS用：Bearer Token認証
                uid = authenticateWithBearerToken(bearerToken);
            } else {
                // 認証ヘッダーが存在しない場合
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing authentication header (X-API-KEY or Authorization)");
            }
            
            request.setAttribute(UID_ATTRIBUTE, uid);
            return true;
            
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Firebase authentication token");
        }
    }
    
    private String authenticateWithApiKey(String apiKey) {
        if (!lambdaApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-API-KEY");
        }
        return LAMBDA_UID;
    }
    
    private String authenticateWithBearerToken(String bearerToken) throws FirebaseAuthException {
        if (!bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Authorization header format. Expected: Bearer <token>");
        }
        
        String idToken = bearerToken.substring(7);
        FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        return firebaseToken.getUid();
    }
}