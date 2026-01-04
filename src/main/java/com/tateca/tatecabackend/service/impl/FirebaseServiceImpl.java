package com.tateca.tatecabackend.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.tateca.tatecabackend.service.FirebaseService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Lazy(value = false)
@Service
public class FirebaseServiceImpl implements FirebaseService {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseServiceImpl.class);

    @Value("${firebase.serviceAccountKey}")
    private String serviceAccountKey;

    @PostConstruct
    public synchronized void initialize() {
        try {
            // Check if FirebaseApp is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                if (serviceAccountKey == null || serviceAccountKey.trim().isEmpty()) {
                    logger.error("Firebase service account key not configured");
                    throw new RuntimeException("FIREBASE_SERVICE_ACCOUNT_KEY environment variable is not set");
                }

                // Skip Firebase initialization in test environment
                if ("mock-service-account-key".equals(serviceAccountKey)) {
                    logger.info("Firebase initialization skipped (test environment)");
                    return;
                }

                InputStream serviceAccountStream = new ByteArrayInputStream(serviceAccountKey.getBytes());

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully");
            } else {
                logger.debug("Firebase already initialized");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    @Override
    public String generateCustomToken(String uid) throws FirebaseAuthException {
        logger.debug("Generating custom Firebase token for user");
        try {
            String token = FirebaseAuth.getInstance().createCustomToken(uid);
            logger.info("Custom Firebase token generated successfully");
            return token;
        } catch (FirebaseAuthException e) {
            logger.error("Failed to generate custom Firebase token: {}", e.getMessage(), e);
            throw e;
        }
    }
}
