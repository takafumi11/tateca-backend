package com.tateca.tatecabackend.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OpenAPI contract tests for the Update User Name endpoint.
 *
 * <p>Each test verifies that the actual HTTP response conforms to the schema and examples
 * defined in {@code openapi/paths/users-userId-update-user-name.yaml}.
 *
 * <p>Allowed error codes per status are derived exclusively from the {@code examples} block
 * in the path spec — each example's {@code value.error_code} is the authoritative source.
 * The former {@code x-error-codes} custom extension has been removed to avoid dual maintenance.
 *
 * <p>Required fields for error responses are read dynamically from
 * {@code openapi/components/schemas/errors/ErrorResponse.yaml} so that schema changes
 * are automatically reflected in these assertions.
 */
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("User Update Name OpenAPI Contract Tests")
class UserUpdateNameOpenApiContractTest extends AbstractIntegrationTest {

    // =========================================================
    // Spec file paths (relative to project root = Gradle working dir)
    // =========================================================

    private static final Path USER_PATH_SPEC      = Path.of("openapi/paths/users-userId-update-user-name.yaml");
    private static final Path ERROR_RESPONSE_SCHEMA = Path.of("openapi/components/schemas/errors/ErrorResponse.yaml");

    private static final String ENDPOINT     = "/users/{userId}";
    private static final String X_UID_HEADER = "x-uid";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final Set<String> APP_REVIEW_STATUS_ENUM =
            Set.of("PENDING", "COMPLETED", "PERMANENTLY_DECLINED");

    // =========================================================
    // Contract metadata — loaded once from spec files
    // =========================================================

    /** Maps HTTP status code (String) → list of allowed error_code values, sourced from examples. */
    private static Map<String, List<String>> allowedErrorCodesByStatus;

    /** Required top-level fields of ErrorResponse, sourced from ErrorResponse.yaml. */
    private static Set<String> requiredErrorFields;

    // =========================================================
    // Test infrastructure
    // =========================================================

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthUserRepository authUserRepository;

    private UUID existingUserId;
    private String authUid;

    @BeforeAll
    static void loadOpenApiContractMetadata() throws IOException {
        allowedErrorCodesByStatus = readAllowedErrorCodesByStatus();
        requiredErrorFields       = readRequiredFieldsFromSchema(ERROR_RESPONSE_SCHEMA);
    }

    @BeforeEach
    void setUp() {
        authUid = "contract-uid-" + System.nanoTime();
        // Mirrors the state produced by AuthUserServiceImpl.createAuthUser:
        // totalLoginCount starts at 1 and appReviewStatus is set to PENDING on creation.
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(authUid)
                .name("Contract Auth User")
                .email("contract-" + System.nanoTime() + "@example.com")
                .totalLoginCount(1)
                .appReviewStatus(AppReviewStatus.PENDING)
                .build();
        authUserRepository.save(authUser);

        existingUserId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .uuid(existingUserId)
                .name("Alice")
                .authUser(authUser)
                .build();
        userRepository.save(user);
        flushAndClear();
    }

    // =========================================================
    // Contract Tests
    // =========================================================

    /**
     * 200 — Verifies the complete UserResponse schema:
     * uuid (UUID format), name, created_at, updated_at, and the full auth_user structure.
     */
    @Test
    @DisplayName("Should satisfy 200 response contract")
    void shouldSatisfy200ResponseContract() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));

        MvcResult result = mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType())
                .as("200 response must have application/json Content-Type")
                .contains("application/json");

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertUserResponseStructure(json);
    }

    /**
     * 400 VALIDATION.FAILED — Error contract plus:
     * errors array must be present, non-empty, each element conforming to FieldError schema.
     * Spec states errors is "only present for validation failures".
     */
    @Test
    @DisplayName("Should satisfy 400 validation error contract — errors array must be present")
    void shouldSatisfy400ValidationErrorContract() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", ""));

        MvcResult result = mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = assertErrorResponseContract(result, "400");

        assertThat(json.has("errors"))
                .as("errors array must be present in a VALIDATION.FAILED response")
                .isTrue();
        JsonNode errors = json.path("errors");
        assertThat(errors.isArray())
                .as("errors must be a JSON array")
                .isTrue();
        assertThat(errors.size())
                .as("errors array must not be empty for a validation failure")
                .isGreaterThan(0);
        errors.forEach(this::assertFieldErrorStructure);
    }

    /**
     * 400 REQUEST.MALFORMED_JSON — Error contract plus:
     * errors array must be absent (spec: "only present for validation failures").
     */
    @Test
    @DisplayName("Should satisfy 400 malformed JSON error contract — errors array must be absent")
    void shouldSatisfy400MalformedJsonErrorContract() throws Exception {
        MvcResult result = mockMvc.perform(authenticatedPatch(existingUserId, "{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = assertErrorResponseContract(result, "400");

        assertThat(json.has("errors"))
                .as("errors array must NOT be present in a REQUEST.MALFORMED_JSON response")
                .isFalse();
    }

    /**
     * 401 — Standard error contract.
     */
    @Test
    @DisplayName("Should satisfy 401 error contract")
    void shouldSatisfy401ErrorContract() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));

        MvcResult result = mockMvc.perform(unauthenticatedPatch(existingUserId, body))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertErrorResponseContract(result, "401");
    }

    /**
     * 404 — Standard error contract.
     */
    @Test
    @DisplayName("Should satisfy 404 error contract")
    void shouldSatisfy404ErrorContract() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));
        UUID nonExistentUserId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(authenticatedPatch(nonExistentUserId, body))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponseContract(result, "404");
    }

    /**
     * 415 — Standard error contract.
     * Triggered by sending a request without Content-Type: application/json.
     */
    @Test
    @DisplayName("Should satisfy 415 error contract")
    void shouldSatisfy415ErrorContract() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));

        MvcResult result = mockMvc.perform(patch(ENDPOINT, existingUserId)
                        .header(X_UID_HEADER, authUid)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnsupportedMediaType())
                .andReturn();

        assertErrorResponseContract(result, "415");
    }

    // =========================================================
    // Structural assertion helpers
    // =========================================================

    /**
     * Verifies the full UserResponse schema (UserResponse.yaml + AuthUserResponse.yaml).
     */
    private void assertUserResponseStructure(JsonNode json) {
        // uuid: string, UUID format
        assertThat(json.path("uuid").isTextual())
                .as("uuid must be a string")
                .isTrue();
        assertThat(UUID_PATTERN.matcher(json.path("uuid").asText()).matches())
                .as("uuid must be a valid UUID format, got: %s", json.path("uuid").asText())
                .isTrue();

        // name: non-blank string
        assertThat(json.path("name").isTextual())
                .as("name must be a string")
                .isTrue();
        assertThat(json.path("name").asText())
                .as("name must not be blank")
                .isNotBlank();

        // created_at: non-blank string
        assertThat(json.path("created_at").isTextual())
                .as("created_at must be a string")
                .isTrue();
        assertThat(json.path("created_at").asText())
                .as("created_at must not be blank")
                .isNotBlank();

        // updated_at: non-blank string
        assertThat(json.path("updated_at").isTextual())
                .as("updated_at must be a string")
                .isTrue();
        assertThat(json.path("updated_at").asText())
                .as("updated_at must not be blank")
                .isNotBlank();

        // auth_user: field must be present; if non-null, structure is verified
        assertThat(json.has("auth_user"))
                .as("auth_user field must be present in the response")
                .isTrue();
        JsonNode authUser = json.path("auth_user");
        if (!authUser.isNull() && !authUser.isMissingNode()) {
            assertAuthUserStructure(authUser);
        }
    }

    /**
     * Verifies the auth_user object structure against AuthUserResponse.yaml.
     */
    private void assertAuthUserStructure(JsonNode authUser) {
        // Non-blank string fields
        for (String field : List.of("uid", "name", "email", "created_at", "updated_at")) {
            assertThat(authUser.path(field).isTextual())
                    .as("auth_user.%s must be a string", field)
                    .isTrue();
            assertThat(authUser.path(field).asText())
                    .as("auth_user.%s must not be blank", field)
                    .isNotBlank();
        }

        // total_login_count: integer >= 1
        // AuthUserServiceImpl.createAuthUser initialises this to 1, so it is always non-null.
        assertThat(authUser.has("total_login_count"))
                .as("auth_user.total_login_count must be present")
                .isTrue();
        assertThat(authUser.path("total_login_count").isNumber())
                .as("auth_user.total_login_count must be a number")
                .isTrue();
        assertThat(authUser.path("total_login_count").asInt())
                .as("auth_user.total_login_count must be >= 1 (set to 1 on creation)")
                .isGreaterThanOrEqualTo(1);

        // Nullable string fields: present but may be null
        for (String field : List.of("last_login_time", "last_app_review_dialog_shown_at")) {
            assertThat(authUser.has(field))
                    .as("auth_user.%s must be present (null or string)", field)
                    .isTrue();
            JsonNode node = authUser.path(field);
            assertThat(node.isNull() || node.isTextual())
                    .as("auth_user.%s must be null or a string", field)
                    .isTrue();
        }

        // app_review_status: non-null enum value
        // AuthUserServiceImpl.createAuthUser initialises this to PENDING, so it is always non-null.
        assertThat(authUser.has("app_review_status"))
                .as("auth_user.app_review_status must be present")
                .isTrue();
        String appReviewStatus = authUser.path("app_review_status").asText();
        assertThat(APP_REVIEW_STATUS_ENUM)
                .as("auth_user.app_review_status must be one of %s, got: %s",
                        APP_REVIEW_STATUS_ENUM, appReviewStatus)
                .contains(appReviewStatus);
    }

    /**
     * Verifies a single element of the errors array against FieldError.yaml.
     * field and message are required for the error to be actionable;
     * rejected_value must be present (any type, including null, is valid per spec).
     */
    private void assertFieldErrorStructure(JsonNode fieldError) {
        assertThat(fieldError.has("field"))
                .as("FieldError.field must be present")
                .isTrue();
        assertThat(fieldError.path("field").isTextual())
                .as("FieldError.field must be a string")
                .isTrue();
        assertThat(fieldError.path("field").asText())
                .as("FieldError.field must not be blank")
                .isNotBlank();

        assertThat(fieldError.has("message"))
                .as("FieldError.message must be present")
                .isTrue();
        assertThat(fieldError.path("message").isTextual())
                .as("FieldError.message must be a string")
                .isTrue();
        assertThat(fieldError.path("message").asText())
                .as("FieldError.message must not be blank")
                .isNotBlank();

        // rejected_value is present in the spec example; any JSON type (including null) is acceptable
        assertThat(fieldError.has("rejected_value"))
                .as("FieldError.rejected_value must be present")
                .isTrue();
    }

    /**
     * Shared error response contract assertion.
     *
     * <p>Verifies:
     * <ul>
     *   <li>Content-Type is application/json</li>
     *   <li>All required fields from ErrorResponse.yaml are present</li>
     *   <li>status field (integer) matches the HTTP status code</li>
     *   <li>error field is a non-blank string</li>
     *   <li>message field is a non-blank string</li>
     *   <li>path field reflects the request URI</li>
     *   <li>error_code is one of the allowed values derived from spec examples</li>
     *   <li>request_id, if present and non-null, is a valid UUID</li>
     * </ul>
     *
     * @return the parsed response body as {@link JsonNode} for additional assertions by the caller
     */
    private JsonNode assertErrorResponseContract(MvcResult result, String statusCode) throws Exception {
        // Content-Type
        assertThat(result.getResponse().getContentType())
                .as("Error response Content-Type must be application/json for status %s", statusCode)
                .contains("application/json");

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .as("Error response body must not be empty for status %s", statusCode)
                .isNotBlank();

        JsonNode json = objectMapper.readTree(body);

        // All required fields from ErrorResponse.yaml
        for (String field : requiredErrorFields) {
            assertThat(json.has(field))
                    .as("Required field '%s' must be present for status %s", field, statusCode)
                    .isTrue();
        }

        // status: integer matching HTTP status code
        assertThat(json.path("status").isInt())
                .as("status must be an integer for status %s", statusCode)
                .isTrue();
        assertThat(json.path("status").asInt())
                .as("status value must match HTTP status code %s", statusCode)
                .isEqualTo(Integer.parseInt(statusCode));

        // error: non-blank string (HTTP reason phrase)
        assertThat(json.path("error").isTextual())
                .as("error must be a string for status %s", statusCode)
                .isTrue();
        assertThat(json.path("error").asText())
                .as("error must not be blank for status %s", statusCode)
                .isNotBlank();

        // message: non-blank string
        assertThat(json.path("message").isTextual())
                .as("message must be a string for status %s", statusCode)
                .isTrue();
        assertThat(json.path("message").asText())
                .as("message must not be blank for status %s", statusCode)
                .isNotBlank();

        // path: must reflect the request URI
        assertThat(json.path("path").isTextual())
                .as("path must be a string for status %s", statusCode)
                .isTrue();
        assertThat(json.path("path").asText())
                .as("path must contain the request URI for status %s", statusCode)
                .startsWith("/users/");

        // error_code: must be one of the allowed values derived from spec examples
        List<String> allowedCodes = allowedErrorCodesByStatus.getOrDefault(statusCode, Collections.emptyList());
        assertThat(allowedCodes)
                .as("Spec examples must define at least one error_code for status %s", statusCode)
                .isNotEmpty();
        String actualCode = json.path("error_code").asText();
        assertThat(actualCode)
                .as("error_code must be one of %s for status %s, but was: %s",
                        allowedCodes, statusCode, actualCode)
                .isIn(allowedCodes);

        // request_id: optional — if present and non-null, must be UUID format
        if (json.has("request_id") && !json.path("request_id").isNull()) {
            String requestId = json.path("request_id").asText();
            assertThat(UUID_PATTERN.matcher(requestId).matches())
                    .as("request_id must be a valid UUID if present, got: %s", requestId)
                    .isTrue();
        }

        return json;
    }

    // =========================================================
    // Request helpers
    // =========================================================

    private MockHttpServletRequestBuilder authenticatedPatch(UUID userId, String body) {
        return patch(ENDPOINT, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(X_UID_HEADER, authUid)
                .content(body);
    }

    private MockHttpServletRequestBuilder unauthenticatedPatch(UUID userId, String body) {
        return patch(ENDPOINT, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body);
    }

    // =========================================================
    // Spec loading — single source of truth
    // =========================================================

    /**
     * Builds the allowed-error-codes map by traversing the examples block in the path spec.
     *
     * <p>For each response status, the method resolves every {@code examples.$ref} to its
     * actual YAML file and extracts {@code value.error_code}. This eliminates the need for
     * the {@code x-error-codes} custom extension.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> readAllowedErrorCodesByStatus() throws IOException {
        Map<String, List<String>> codesByStatus = new LinkedHashMap<>();
        Yaml yaml = new Yaml();
        Path specDir = USER_PATH_SPEC.getParent();

        try (InputStream in = Files.newInputStream(USER_PATH_SPEC)) {
            Map<String, Object> root     = yaml.load(in);
            Map<String, Object> patchOp  = (Map<String, Object>) root.get("patch");
            Map<String, Object> responses = (Map<String, Object>) patchOp.get("responses");

            for (Map.Entry<String, Object> responseEntry : responses.entrySet()) {
                String status = String.valueOf(responseEntry.getKey());
                if (!(responseEntry.getValue() instanceof Map<?, ?> responseObj)) continue;

                Map<String, Object> responseMap = (Map<String, Object>) responseObj;
                Map<String, Object> content     = (Map<String, Object>) responseMap.get("content");
                if (content == null) continue;

                Map<String, Object> appJson = (Map<String, Object>) content.get("application/json");
                if (appJson == null) continue;

                Map<String, Object> examples = (Map<String, Object>) appJson.get("examples");
                if (examples == null) continue;

                List<String> codes = new ArrayList<>();
                for (Map.Entry<String, Object> exEntry : examples.entrySet()) {
                    if (!(exEntry.getValue() instanceof Map<?, ?> exObj)) continue;
                    Map<String, Object> exMap = (Map<String, Object>) exObj;
                    String ref = (String) exMap.get("$ref");
                    if (ref == null) continue;

                    Path examplePath = specDir.resolve(ref).normalize();
                    try (InputStream exIn = Files.newInputStream(examplePath)) {
                        Map<String, Object> exRoot = yaml.load(exIn);
                        Map<String, Object> value  = (Map<String, Object>) exRoot.get("value");
                        if (value == null) continue;
                        String errorCode = (String) value.get("error_code");
                        if (errorCode != null) codes.add(errorCode);
                    }
                }

                if (!codes.isEmpty()) codesByStatus.put(status, codes);
            }
        }

        return codesByStatus;
    }

    /**
     * Reads the {@code required} field list from a JSON Schema YAML file.
     */
    @SuppressWarnings("unchecked")
    private static Set<String> readRequiredFieldsFromSchema(Path schemaPath) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(schemaPath)) {
            Map<String, Object> root = yaml.load(in);
            Object requiredObj = root.get("required");
            if (requiredObj instanceof List<?> requiredList) {
                return requiredList.stream().map(String::valueOf).collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }
}
