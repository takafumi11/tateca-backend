package com.tateca.tatecabackend.util;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for masking Personally Identifiable Information (PII) in logs.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Development environment (dev): Returns original values (no masking)</li>
 *   <li>Production environment (prod): Masks sensitive data</li>
 * </ul>
 *
 * <p>Masking Strategy:
 * <ul>
 *   <li>Email: Shows first character + domain, masks rest (e.g., "u***@example.com")</li>
 *   <li>UID/UUID: Shows first 8 characters, masks rest (e.g., "12345678-****")</li>
 *   <li>Token: Shows first 4 characters, masks rest (e.g., "abcd****")</li>
 *   <li>Hashed ID: Creates consistent hash for user identification across logs</li>
 * </ul>
 */
@Component
@NoArgsConstructor
public class PiiMaskingUtil {

    private static final String MASK = "****";
    private static String activeProfile = "dev"; // Default to dev

    /**
     * Set active Spring profile at runtime.
     * Called by Spring configuration.
     */
    @Value("${spring.profiles.active:dev}")
    public void setActiveProfile(String profile) {
        activeProfile = profile;
    }

    /**
     * Check if masking should be applied.
     * Masking is disabled in dev environment for easier debugging.
     */
    private static boolean shouldMask() {
        return !"dev".equals(activeProfile);
    }

    /**
     * Mask email address.
     * Dev: "user@example.com" -> "user@example.com"
     * Prod: "user@example.com" -> "u***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty() || !shouldMask()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return MASK;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() == 1) {
            return localPart + MASK + domain;
        }

        return localPart.charAt(0) + MASK + domain;
    }

    /**
     * Mask UID or UUID.
     * Dev: "abcd1234-5678-90ef-ghij-klmnopqrstuv" -> "abcd1234-5678-90ef-ghij-klmnopqrstuv"
     * Prod: "abcd1234-5678-90ef-ghij-klmnopqrstuv" -> "abcd1234-****"
     */
    public static String maskUid(String uid) {
        if (uid == null || uid.isEmpty() || !shouldMask()) {
            return uid;
        }

        if (uid.length() <= 8) {
            return MASK;
        }

        return uid.substring(0, 8) + "-" + MASK;
    }

    /**
     * Mask UUID object.
     */
    public static String maskUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return maskUid(uuid.toString());
    }

    /**
     * Mask token (join token, API key, etc.).
     * Dev: "abcd1234efgh5678" -> "abcd1234efgh5678"
     * Prod: "abcd1234efgh5678" -> "abcd****"
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty() || !shouldMask()) {
            return token;
        }

        if (token.length() <= 4) {
            return MASK;
        }

        return token.substring(0, 4) + MASK;
    }

    /**
     * Create a consistent hashed identifier for user tracking across logs.
     * This allows correlating user actions without exposing PII.
     *
     * Dev: "user123" -> "user123"
     * Prod: "user123" -> "hash_a3f5b9c1..."
     */
    public static String hashUserId(String userId) {
        if (userId == null || userId.isEmpty() || !shouldMask()) {
            return userId;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
            String base64Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            // Take first 12 characters for brevity
            return "hash_" + base64Hash.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return MASK;
        }
    }

    /**
     * Mask generic sensitive data with custom prefix.
     * Useful for custom sensitive fields.
     */
    public static String maskSensitive(String data, int visibleChars) {
        if (data == null || data.isEmpty() || !shouldMask()) {
            return data;
        }

        if (data.length() <= visibleChars) {
            return MASK;
        }

        return data.substring(0, visibleChars) + MASK;
    }
}
