package com.moneyme.moneymebackend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

public class FirebaseAuthHelper {
    static public FirebaseToken verifyIdToken(String bearerToken) throws FirebaseAuthException {
        if (bearerToken.startsWith("Bearer ")) {
            String idToken = bearerToken.substring(7);
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } else {
            throw new RuntimeException("Invalid token format");
        }
    }
}