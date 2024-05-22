package com.moneyme.moneymebackend.service.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FirebaseAuthHelper {
    static public FirebaseToken verifyIdToken(String bearerToken) throws ResponseStatusException  {
        String idToken = getIdToken(bearerToken);
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Bearer Token");
        }
    }

    static private String getIdToken(String bearerToken) throws ResponseStatusException {
        if (bearerToken.startsWith("Bearer ")) {
          return bearerToken.substring(7);
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing Authorization header");
        }
    }
}