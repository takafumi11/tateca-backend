package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance scenario tests for Update User Name.
 *
 * <p>Each test maps directly to an Acceptance Criterion in
 * {@code docs/specs/update-user-name/requirements.md}.
 *
 * <p>All setup and verification is performed exclusively through HTTP endpoints,
 * mirroring the actual client flow. No direct repository or database access is used.
 *
 * <p>Client flow:
 * <ol>
 *   <li>POST /auth/users — create authenticated user</li>
 *   <li>POST /groups — create group (implicitly creates User entity)</li>
 *   <li>PATCH /users/{userId} — update user name (test target)</li>
 *   <li>GET /groups/{groupId} — verify updated name via group detail response</li>
 * </ol>
 */
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Update User Name — Acceptance Scenario Tests")
class UpdateUserNameScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String authUid;
    private String userId;
    private String groupId;

    @BeforeEach
    void setUp() throws Exception {
        authUid = "scenario-uid-" + System.nanoTime();

        // Step 1: Create auth user via API
        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, authUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", authUid + "@example.com"))))
                .andExpect(status().isCreated());

        // Step 2: Create group via API (creates User entity as host)
        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, authUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", "Test Group",
                                "host_name", "Alice",
                                "participants_name", List.of("Bob")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();

        // Find the host user's UUID from the group response
        JsonNode users = groupResponse.path("users");
        for (JsonNode user : users) {
            if (user.path("auth_user") != null && !user.path("auth_user").isNull()
                    && !user.path("auth_user").isMissingNode()) {
                userId = user.path("uuid").asText();
                break;
            }
        }
    }

    /**
     * Retrieves the display name of the test user by calling GET /groups/{groupId}
     * and finding the user with matching userId in the response.
     */
    private String getUserNameViaGroupApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/{groupId}", groupId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, authUid))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode user : response.path("users")) {
            if (userId.equals(user.path("uuid").asText())) {
                return user.path("name").asText();
            }
        }
        throw new AssertionError("User " + userId + " not found in group response");
    }

    /**
     * Retrieves the updated_at of the test user by calling GET /groups/{groupId}.
     */
    private String getUserUpdatedAtViaGroupApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/{groupId}", groupId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, authUid))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode user : response.path("users")) {
            if (userId.equals(user.path("uuid").asText())) {
                return user.path("updated_at").asText();
            }
        }
        throw new AssertionError("User " + userId + " not found in group response");
    }

    // =========================================================
    // Req 1: 表示名の更新（正常系）
    // =========================================================

    @Nested
    @DisplayName("Req1: Normal update")
    class Req1_NormalUpdate {

        @Test
        @DisplayName("AC1: Should update name with normalized value and return it")
        void ac1_shouldUpdateNameAndReturnIt() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Charlie"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Charlie"));
        }

        @Test
        @DisplayName("AC2: Should persist updated name — verified via GET group detail")
        void ac2_shouldPersistUpdatedName() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Charlie"))))
                    .andExpect(status().isOk());

            assertThat(getUserNameViaGroupApi()).isEqualTo("Charlie");
        }
    }

    // =========================================================
    // Req 2: 入力正規化とバリデーション
    // =========================================================

    @Nested
    @DisplayName("Req2: Normalization and validation")
    class Req2_NormalizationAndValidation {

        @Test
        @DisplayName("AC1: Should trim whitespace — verified via PATCH response and GET group detail")
        void ac1_shouldTrimAndPersistNormalizedValue() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "   Charlie   "))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Charlie"));

            assertThat(getUserNameViaGroupApi()).isEqualTo("Charlie");
        }

        @ParameterizedTest(name = "[{index}] user_name = \"{0}\"")
        @DisplayName("AC2: Should reject blank values (empty or whitespace-only)")
        @ValueSource(strings = {"", "   "})
        void ac2_shouldRejectBlankValues(String blankName) throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", blankName))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));
        }

        @Test
        @DisplayName("AC2: Should reject when user_name key is missing from request")
        void ac2_shouldRejectMissingKey() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));
        }

        @Test
        @DisplayName("AC2: Should reject when user_name is null")
        void ac2_shouldRejectNullValue() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{\"user_name\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));
        }

        @Test
        @DisplayName("AC3: Should reject name exceeding 50 characters after normalization")
        void ac3_shouldRejectNameExceedingMaxLength() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "A".repeat(51)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));
        }

        @Test
        @DisplayName("AC1+AC3: Should accept 50-char name with surrounding whitespace (trim before length check)")
        void ac1ac3_shouldAcceptMaxLengthNameWithSurroundingWhitespace() throws Exception {
            String nameWith50Chars = "A".repeat(50);

            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "  " + nameWith50Chars + "  "))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(nameWith50Chars));
        }

    }

    // =========================================================
    // Req 3: 認証と認可
    // =========================================================

    @Nested
    @DisplayName("Req3: Authentication and authorization")
    class Req3_AuthenticationAndAuthorization {

        @Test
        @DisplayName("AC1+AC2: Should reject unauthenticated request with 401")
        void ac1ac2_shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Charlie"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error_code").value(anyOf(
                            is("AUTH.MISSING_CREDENTIALS"),
                            is("AUTH.INVALID_FORMAT"),
                            is("AUTH.INVALID_TOKEN")
                    )));
        }

        @Test
        @DisplayName("AC3: Should reject when authenticated user is not the resource owner")
        void ac3_shouldRejectWhenNotResourceOwner() throws Exception {
            // Create another authenticated user via API
            String otherUid = "other-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, otherUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", otherUid + "@example.com"))))
                    .andExpect(status().isCreated());

            // Attempt to update the first user's name with the other user's credentials
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, otherUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Hacked"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error_code").value("USER.FORBIDDEN"));

            // Verify original name is unchanged via GET group detail
            assertThat(getUserNameViaGroupApi()).isEqualTo("Alice");
        }
    }

    // =========================================================
    // Req 4: 冪等性（同値更新）
    // =========================================================

    @Nested
    @DisplayName("Req4: Idempotent same-value update")
    class Req4_IdempotentSameValueUpdate {

        @Test
        @DisplayName("AC1: Should succeed when updating with same normalized value")
        void ac1_shouldSucceedOnSameValueUpdate() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Alice"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Alice"));
        }

        @Test
        @DisplayName("AC2: Should not create observable inconsistencies — name unchanged via GET group detail")
        void ac2_shouldNotCreateInconsistencies() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Alice"))))
                    .andExpect(status().isOk());

            assertThat(getUserNameViaGroupApi()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("AC3: Should not change updated_at on same-value update")
        void ac3_shouldNotChangeUpdatedAtOnSameValueUpdate() throws Exception {
            String updatedAtBefore = getUserUpdatedAtViaGroupApi();

            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Alice"))))
                    .andExpect(status().isOk());

            String updatedAtAfter = getUserUpdatedAtViaGroupApi();
            assertThat(updatedAtAfter).isEqualTo(updatedAtBefore);
        }
    }

    // =========================================================
    // Req 5: リソース不存在
    // =========================================================

    @Nested
    @DisplayName("Req5: Resource not found")
    class Req5_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return not found for non-existent userId")
        void ac1_shouldReturnNotFoundForNonExistentUser() throws Exception {
            mockMvc.perform(patch("/users/{userId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Charlie"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_FOUND"));
        }
    }

    // =========================================================
    // Technical Edge Cases (outside requirements.md scope)
    // =========================================================

    @Nested
    @DisplayName("Technical: Request format edge cases")
    class TechnicalEdgeCases {

        @Test
        @DisplayName("Should reject malformed JSON")
        void shouldRejectMalformedJson() throws Exception {
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("REQUEST.MALFORMED_JSON"));
        }
    }
}
