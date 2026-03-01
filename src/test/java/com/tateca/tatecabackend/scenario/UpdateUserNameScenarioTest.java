package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance scenario tests for Update User Name.
 *
 * <p>Each test maps directly to a scenario defined in
 * {@code docs/specs/update-user-name/scenario-tests.md}.
 * P0 scenarios are mandatory and must always pass in CI.
 * P1 scenarios are optional extended cases.
 *
 * <p>Scope: observable HTTP behaviour only.
 * Internal implementation details (repository call counts, etc.) are not verified here.
 */
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Update User Name Scenario Tests")
class UpdateUserNameScenarioTest extends AbstractIntegrationTest {

    private static final String ENDPOINT = "/users/{userId}";
    private static final String X_UID_HEADER = "x-uid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUserRepository authUserRepository;

    private UUID existingUserId;
    private String authUid;

    @BeforeEach
    void setUp() {
        authUid = "scenario-uid-" + System.nanoTime();
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(authUid)
                .name("Scenario Auth User")
                .email("scenario-" + System.nanoTime() + "@example.com")
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
    // P0 Scenarios — Must Pass (requirements.md Req 1–5)
    // =========================================================

    /**
     * SCN-001 — 正常更新 (Req 1)
     * Response returns updated name; subsequent fetch confirms persistence.
     */
    @Test
    @DisplayName("SCN-001: Should update name and persist the change")
    void scn001_shouldUpdateNameAndPersistChange() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(existingUserId.toString()))
                .andExpect(jsonPath("$.name").value("Bob"));

        flushAndClear();
        assertThat(userRepository.findById(existingUserId).orElseThrow().getName())
                .isEqualTo("Bob");
    }

    /**
     * SCN-002 — 前後空白トリム (Req 2-1)
     * Leading/trailing whitespace is stripped before storage.
     * Response and subsequent fetch both return the trimmed value.
     */
    @Test
    @DisplayName("SCN-002: Should trim leading and trailing whitespace before storing")
    void scn002_shouldTrimWhitespaceAndPersistNormalizedName() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "   Bob   "));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));

        flushAndClear();
        assertThat(userRepository.findById(existingUserId).orElseThrow().getName())
                .isEqualTo("Bob");
    }

    /**
     * SCN-003 — 同値更新の冪等 (Req 4)
     * Sending the current name again succeeds, the name is unchanged,
     * and no duplicate records are created (no observable inconsistency).
     */
    @Test
    @DisplayName("SCN-003: Should succeed on same-value update without creating inconsistencies")
    void scn003_shouldHandleSameValueUpdateIdempotently() throws Exception {
        long userCountBefore = userRepository.count();
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Alice"));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));

        flushAndClear();
        assertThat(userRepository.findById(existingUserId).orElseThrow().getName())
                .isEqualTo("Alice");
        assertThat(userRepository.count())
                .as("No duplicate user records should be created on same-value update")
                .isEqualTo(userCountBefore);
    }

    /**
     * SCN-004 — バリデーション: 空値 (Req 2-2)
     * Both empty string and whitespace-only inputs are rejected with VALIDATION.FAILED.
     * Spec: "空値（"" または空白のみ）を送信する"
     */
    @ParameterizedTest(name = "[{index}] user_name = \"{0}\"")
    @DisplayName("SCN-004: Should reject blank name with VALIDATION.FAILED")
    @ValueSource(strings = {"", "   "})
    void scn004_shouldRejectBlankName(String blankName) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", blankName));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));
    }

    /**
     * SCN-005 — バリデーション: 長さ超過 (Req 2-3)
     * A name exceeding 50 characters after normalization is rejected.
     */
    @Test
    @DisplayName("SCN-005: Should reject name longer than 50 characters after normalization")
    void scn005_shouldRejectNameExceedingMaxLength() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "A".repeat(51)));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));
    }

    /**
     * SCN-006 — 未認証拒否 (Req 3)
     * Requests without authentication credentials are rejected with 401.
     * Spec permits any of the three auth error codes.
     */
    @Test
    @DisplayName("SCN-006: Should reject unauthenticated request with 401")
    void scn006_shouldRejectUnauthenticatedRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));

        mockMvc.perform(unauthenticatedPatch(existingUserId, body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error_code").value(anyOf(
                        is("AUTH.MISSING_CREDENTIALS"),
                        is("AUTH.INVALID_FORMAT"),
                        is("AUTH.INVALID_TOKEN")
                )));
    }

    /**
     * SCN-007 — 不正JSON (Req 1 / HTTP contract)
     * Syntactically invalid JSON body is rejected with REQUEST.MALFORMED_JSON.
     */
    @Test
    @DisplayName("SCN-007: Should reject malformed JSON with REQUEST.MALFORMED_JSON")
    void scn007_shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(authenticatedPatch(existingUserId, "{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("REQUEST.MALFORMED_JSON"));
    }

    /**
     * SCN-008 — ユーザー不存在 (Req 5)
     * An authenticated request for a non-existent userId returns USER.NOT_FOUND.
     */
    @Test
    @DisplayName("SCN-008: Should return USER.NOT_FOUND for non-existent userId")
    void scn008_shouldReturnUserNotFoundForNonExistentUser() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));
        UUID nonExistentUserId = UUID.randomUUID();

        mockMvc.perform(authenticatedPatch(nonExistentUserId, body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("USER.NOT_FOUND"));
    }

    // =========================================================
    // P1 Scenarios — Optional / Extended
    // =========================================================

    /**
     * P1-SCN-001 — 境界値: 最小長（1文字）
     * A single-character name (after normalization) is accepted.
     */
    @Test
    @DisplayName("P1-SCN-001: Should accept minimum-length name (1 character after normalization)")
    void p1scn001_shouldAcceptSingleCharName() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "A"));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("A"));
    }

    /**
     * P1-SCN-002 — 境界値: 最大長（50文字）
     * A 50-character name (after normalization) is accepted.
     */
    @Test
    @DisplayName("P1-SCN-002: Should accept maximum-length name (50 characters after normalization)")
    void p1scn002_shouldAcceptMaxLengthName() throws Exception {
        String maxName = "A".repeat(50);
        String body = objectMapper.writeValueAsString(Map.of("user_name", maxName));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(maxName));
    }

    /**
     * P1-SCN-003 — 境界値: トリム後50文字（受理）
     * Confirms that whitespace trimming occurs BEFORE length validation:
     * a 50-character name surrounded by whitespace is accepted (not rejected as too long).
     */
    @Test
    @DisplayName("P1-SCN-003: Should accept 50-character name with surrounding whitespace (trim before length check)")
    void p1scn003_shouldAcceptMaxLengthNameWithSurroundingWhitespace() throws Exception {
        String nameWith50Chars = "A".repeat(50);
        String body = objectMapper.writeValueAsString(Map.of("user_name", "  " + nameWith50Chars + "  "));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(nameWith50Chars));
    }

    /**
     * P1-SCN-004 — Unicode・絵文字
     * Names containing multi-byte Unicode characters and emoji are stored and returned correctly.
     */
    @Test
    @DisplayName("P1-SCN-004: Should persist Unicode and emoji characters correctly")
    void p1scn004_shouldPersistUnicodeAndEmojiName() throws Exception {
        String unicodeName = "田中 😊";
        String body = objectMapper.writeValueAsString(Map.of("user_name", unicodeName));

        mockMvc.perform(authenticatedPatch(existingUserId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(unicodeName));

        flushAndClear();
        assertThat(userRepository.findById(existingUserId).orElseThrow().getName())
                .isEqualTo(unicodeName);
    }

    /**
     * P1-SCN-005 — Content-Type 不正（415）
     * Requests without application/json Content-Type are rejected with
     * 415 Unsupported Media Type and REQUEST.UNSUPPORTED_MEDIA_TYPE error code.
     */
    @Test
    @DisplayName("P1-SCN-005: Should return 415 when Content-Type is not application/json")
    void p1scn005_shouldRejectUnsupportedMediaType() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("user_name", "Bob"));

        mockMvc.perform(patch(ENDPOINT, existingUserId)
                        .header(X_UID_HEADER, authUid)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error_code").value("REQUEST.UNSUPPORTED_MEDIA_TYPE"));
    }

    // =========================================================
    // Helpers
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
}
