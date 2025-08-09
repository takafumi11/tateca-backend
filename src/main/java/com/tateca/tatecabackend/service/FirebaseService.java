package com.tateca.tatecabackend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@Lazy(false) // Lazy初期化を無効化、即座に初期化
public class FirebaseService {

    @PostConstruct
    public synchronized void initialize() {
        try {
            // Check if FirebaseApp is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                String serviceAccountKey = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY");
                if (serviceAccountKey == null || serviceAccountKey.trim().isEmpty()) {
                    throw new RuntimeException("FIREBASE_SERVICE_ACCOUNT_KEY environment variable is not set");
                }
                
                InputStream serviceAccountStream = new ByteArrayInputStream(serviceAccountKey.getBytes());

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    public String generateCustomToken(String uid) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().createCustomToken(uid);
    }
}