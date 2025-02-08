package com.moneyme.moneymebackend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseService {

    @PostConstruct
    public void initialize() {
        try {
            String serviceAccountKey = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY");
            System.out.println("FIREBASE_SERVICE_ACCOUNT_KEY: " + serviceAccountKey);
            InputStream serviceAccountStream = new ByteArrayInputStream(serviceAccountKey.getBytes());

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}