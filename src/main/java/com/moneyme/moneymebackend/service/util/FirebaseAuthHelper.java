package com.moneyme.moneymebackend.service.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.moneyme.moneymebackend.exception.CustomResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class FirebaseAuthHelper {
    static public FirebaseToken verifyIdToken(String bearerToken, String apiName, ZonedDateTime requestTime) throws CustomResponseStatusException {
        String idToken = getIdToken(bearerToken, apiName, requestTime);
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new CustomResponseStatusException(requestTime, apiName, "unknown", "Invalid Bearer Token", HttpStatus.UNAUTHORIZED);
        }
    }

    static private String getIdToken(String bearerToken, String apiName, ZonedDateTime requestTime) throws CustomResponseStatusException {
        if (bearerToken == null || bearerToken.isEmpty()) {
            throw new CustomResponseStatusException(requestTime, apiName, "unknown", "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        } else if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        } else {
            throw new CustomResponseStatusException(requestTime, apiName, "unknown", "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }
    }
}