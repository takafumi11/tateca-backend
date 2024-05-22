package com.moneyme.moneymebackend.service.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

public class FirebaseAuthHelper {
    static public FirebaseToken verifyIdToken(String bearerToken) throws FirebaseAuthException  {
        String idToken = getIdToken(bearerToken);
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }

    static private String getIdToken(String bearerToken) throws IllegalArgumentException {
        if (bearerToken.startsWith("Bearer ")) {
          return bearerToken.substring(7);
        } else {
            throw new IllegalArgumentException("Invalid token format");
        }
    }
}