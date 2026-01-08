package com.tateca.tatecabackend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for masking sensitive fields in JSON request/response bodies.
 *
 * <p>Masking Strategy:
 * <ul>
 *   <li>Masks PII fields: userId, uid, email, password, token, apiKey, etc.</li>
 *   <li>Preserves business data: amount, description, transactionType, status, etc.</li>
 *   <li>Works recursively for nested objects and arrays</li>
 *   <li>Development environment (dev): No masking applied</li>
 *   <li>Production environment (prod): Masks sensitive fields</li>
 * </ul>
 */
@Slf4j
public class JsonBodyMaskingUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * List of field names that should be masked in logs.
     * These are typically PII (Personally Identifiable Information) fields.
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        // User identifiers
        "userId", "user_id", "uid", "uuid",

        // Contact information
        "email", "emailAddress", "email_address",
        "phone", "phoneNumber", "phone_number",

        // Authentication
        "password", "passwd", "currentPassword", "newPassword",
        "token", "accessToken", "refreshToken", "idToken",
        "access_token", "refresh_token", "id_token",
        "joinToken", "join_token",

        // API credentials
        "apiKey", "api_key", "secretKey", "secret_key",
        "authorization"
    );

    /**
     * Mask sensitive fields in JSON body.
     * If the body is not valid JSON, returns the original string.
     *
     * @param jsonBody JSON string to mask
     * @return Masked JSON string, or original string if parsing fails
     */
    public static String maskJsonBody(String jsonBody) {
        if (jsonBody == null || jsonBody.isEmpty()) {
            return jsonBody;
        }

        // Skip masking in dev environment
        if (!PiiMaskingUtil.shouldMask()) {
            return jsonBody;
        }

        try {
            // Parse JSON to Map/List
            Object parsed = OBJECT_MAPPER.readValue(jsonBody, Object.class);

            // Mask sensitive fields recursively
            Object masked = maskObject(parsed);

            // Convert back to JSON string
            return OBJECT_MAPPER.writeValueAsString(masked);

        } catch (Exception e) {
            // If JSON parsing fails, return original string
            // This can happen for non-JSON bodies (plain text, XML, etc.)
            log.debug("Failed to parse JSON for masking, returning original body", e);
            return jsonBody;
        }
    }

    /**
     * Recursively mask sensitive fields in an object.
     */
    @SuppressWarnings("unchecked")
    private static Object maskObject(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Map<String, Object> maskedMap = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (isSensitiveField(key)) {
                    // Mask the value based on its type
                    maskedMap.put(key, maskValue(value));
                } else {
                    // Recursively process nested objects
                    maskedMap.put(key, maskObject(value));
                }
            }

            return maskedMap;

        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            List<Object> maskedList = new ArrayList<>();

            for (Object item : list) {
                maskedList.add(maskObject(item));
            }

            return maskedList;

        } else {
            // Primitive types, strings, numbers, etc. - return as is
            return obj;
        }
    }

    /**
     * Check if a field name is sensitive and should be masked.
     */
    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        // Case-insensitive matching
        String lowerFieldName = fieldName.toLowerCase();

        return SENSITIVE_FIELDS.stream()
            .anyMatch(sensitive -> lowerFieldName.equals(sensitive.toLowerCase()));
    }

    /**
     * Mask a value using appropriate PiiMaskingUtil method.
     */
    private static String maskValue(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();

        // Detect value type and apply appropriate masking
        if (stringValue.contains("@")) {
            // Likely an email
            return PiiMaskingUtil.maskEmail(stringValue);
        } else if (stringValue.length() > 20 && stringValue.contains("-")) {
            // Likely a UUID
            return PiiMaskingUtil.maskUid(stringValue);
        } else {
            // Default token masking
            return PiiMaskingUtil.maskToken(stringValue);
        }
    }
}
