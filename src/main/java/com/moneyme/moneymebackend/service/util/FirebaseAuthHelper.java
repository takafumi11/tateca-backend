package com.moneyme.moneymebackend.service.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.moneyme.moneymebackend.exception.CustomResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;

public class FirebaseAuthHelper {
    static public FirebaseToken verifyIdToken(String bearerToken) throws CustomResponseStatusException {
        String idToken = getIdToken(bearerToken);
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Bearer Token");
        }
    }

    static private String getIdToken(String bearerToken) throws CustomResponseStatusException {
        if (bearerToken == null || bearerToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Authorization header");
        } else if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Authorization header");
        }
    }
}