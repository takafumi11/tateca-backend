package com.tateca.tatecabackend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Lazy(value = false)
@Service
public class FirebaseService {

    @Value("${firebase.serviceAccountKey}")
    private String serviceAccountKey;

    @PostConstruct
    public synchronized void initialize() {
        try {
            // Check if FirebaseApp is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                if (serviceAccountKey == null || serviceAccountKey.trim().isEmpty()) {
                    throw new RuntimeException("FIREBASE_SERVICE_ACCOUNT_KEY environment variable is not set");
                }

                // Skip Firebase initialization in test environment
                if ("mock-service-account-key".equals(serviceAccountKey)) {
                    System.out.println("Firebase initialization skipped (test environment)");
                    return;
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