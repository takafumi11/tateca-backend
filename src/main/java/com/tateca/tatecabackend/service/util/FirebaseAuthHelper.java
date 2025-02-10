package com.tateca.tatecabackend.service.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FirebaseAuthHelper {
    static public FirebaseToken verifyIdToken(String bearerToken) throws FirebaseAuthException {
        if (bearerToken == null || bearerToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Authorization header");
        }

        if (!bearerToken.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Authorization header format");
        }

        String idToken = bearerToken.substring(7);
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}