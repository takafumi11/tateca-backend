package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Update Review Preferences — Acceptance Scenario Tests")
class UpdateReviewPreferencesScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String authUid;

    @BeforeEach
    void setUp() throws Exception {
        authUid = "scenario-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, authUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", authUid + "@example.com"))))
                .andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("Req1: アプリレビュー設定の更新")
    class Req1_UpdateReviewPreferences {

        @Test
        @DisplayName("AC1: 有効なステータスで更新するとアプリレビューステータスが更新される")
        void ac1_shouldUpdateAppReviewStatus() throws Exception {
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            // GET で永続化を確認
            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));
        }

        @Test
        @DisplayName("AC2: 更新時にレビューダイアログ最終表示日時が現在日時に更新される")
        void ac2_shouldUpdateLastAppReviewDialogShownAt() throws Exception {
            MvcResult result = mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("last_app_review_dialog_shown_at").asText()).isNotEmpty();
            assertThat(response.path("last_app_review_dialog_shown_at").isNull()).isFalse();
        }

        @Test
        @DisplayName("AC3: 3種類すべてのステータスで更新が受け入れられる")
        void ac3_shouldAcceptAllThreeStatuses() throws Exception {
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("app_review_status", "PENDING"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of("app_review_status", "PERMANENTLY_DECLINED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("PERMANENTLY_DECLINED"));
        }
    }

    @Nested
    @DisplayName("Req2: 入力バリデーション")
    class Req2_InputValidation {

        @Test
        @DisplayName("AC1: ステータスが未設定の場合、入力不備として拒否される")
        void ac1_shouldRejectMissingStatus() throws Exception {
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AC1: ステータスが null の場合、入力不備として拒否される")
        void ac1_shouldRejectNullStatus() throws Exception {
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{\"app_review_status\": null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("AC2: 定義された3種類以外のステータスの場合、入力不備として拒否される")
        void ac2_shouldRejectInvalidEnumValue() throws Exception {
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content("{\"app_review_status\": \"INVALID_STATUS\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Req3: リソース不存在")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: 認証ユーザーレコードが存在しない場合、リソース不在として拒否される")
        void ac1_shouldRejectWhenAuthUserNotFound() throws Exception {
            String nonExistentUid = "non-existent-uid-" + System.nanoTime();

            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, nonExistentUid)
                            .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"));
        }
    }
}
