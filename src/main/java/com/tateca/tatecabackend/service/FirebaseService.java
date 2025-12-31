package com.tateca.tatecabackend.service;

import com.google.firebase.auth.FirebaseAuthException;

public interface FirebaseService {
    String generateCustomToken(String uid) throws FirebaseAuthException;
}
