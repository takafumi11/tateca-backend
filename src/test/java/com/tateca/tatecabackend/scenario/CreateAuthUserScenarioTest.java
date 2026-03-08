package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Create Auth User — Acceptance Scenario Tests")
class CreateAuthUserScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;

    private String authUid;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        authUid = "scenario-uid-" + System.nanoTime();
    }

    @Nested
    @DisplayName("Req1: 認証ユーザーの作成")
    class Req1_CreateAuthUser {

        @Test
        @DisplayName("AC1: バリデーション通過したメールで作成すると認証ユーザーが作成される")
        void ac1_shouldCreateAuthUserWithValidEmail() throws Exception {
            String email = authUid + "@example.com";

            MvcResult result = mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", email))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(authUid))
                    .andExpect(jsonPath("$.email").value(email))
                    .andReturn();

            // 作成後に GET で存在確認
            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(authUid));
        }

        @Test
        @DisplayName("AC2: 作成時にログイン回数が1、アプリレビューステータスが保留に初期化される")
        void ac2_shouldInitializeDefaults() throws Exception {
            MvcResult result = mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", authUid + "@example.com"))))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("total_login_count").asInt()).isEqualTo(1);
            assertThat(response.path("app_review_status").asText()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("AC3: 作成時に最終ログイン日時が設定される")
        void ac3_shouldSetLastLoginTime() throws Exception {
            MvcResult result = mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", authUid + "@example.com"))))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("last_login_time").asText()).isNotEmpty();
            assertThat(response.path("last_login_time").isNull()).isFalse();
        }
    }

    @Nested
    @DisplayName("Req2: 入力バリデーション")
    class Req2_InputValidation {

        @Test
        @DisplayName("AC1: メールアドレスが空の場合、入力不備として拒否される")
        void ac1_shouldRejectEmptyEmail() throws Exception {
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AC1: メールアドレスが空白のみの場合、入力不備として拒否される")
        void ac1_shouldRejectWhitespaceOnlyEmail() throws Exception {
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", "   "))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AC1: メールアドレスが未設定の場合、入力不備として拒否される")
        void ac1_shouldRejectMissingEmail() throws Exception {
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AC2: メールアドレスが255文字を超える場合、入力不備として拒否される")
        void ac2_shouldRejectEmailExceeding255Characters() throws Exception {
            String longEmail = "a".repeat(244) + "@example.com";

            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", longEmail))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Req3: メールアドレスの一意性")
    class Req3_EmailUniqueness {

        @Test
        @DisplayName("AC1: 既に登録済みのメールアドレスで作成すると重複として拒否される")
        void ac1_shouldRejectDuplicateEmail() throws Exception {
            String email = authUid + "@example.com";

            // 1回目: 成功
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", email))))
                    .andExpect(status().isCreated());

            // 2回目: 同じメールアドレスで別 UID → 重複拒否
            String anotherUid = "another-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, anotherUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", email))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("AUTH_USER.EMAIL_DUPLICATE"));
        }
    }
}
