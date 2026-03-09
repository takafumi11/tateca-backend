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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Get Auth User — Acceptance Scenario Tests")
class GetAuthUserScenarioTest extends AbstractIntegrationTest {

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
    @DisplayName("Req1: 認証ユーザー情報の取得とログイン記録更新")
    class Req1_GetAuthUserAndLoginRecord {

        @Test
        @DisplayName("AC1: 存在する UID で取得すると認証ユーザー情報が返される")
        void ac1_shouldReturnAuthUserInfo() throws Exception {
            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(authUid))
                    .andExpect(jsonPath("$.email").value(authUid + "@example.com"));
        }

        @Test
        @DisplayName("AC2: 取得時にログイン回数が1加算され最終ログイン日時が更新される")
        void ac2_shouldIncrementLoginCountAndUpdateTimestamp() throws Exception {
            MvcResult createResult = mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
            assertThat(response.path("total_login_count").asInt()).isEqualTo(2);
            assertThat(response.path("last_login_time").asText()).isNotEmpty();
        }

        @Test
        @DisplayName("AC3: 複数回取得するとログイン回数が累積的に加算される")
        void ac3_shouldCumulativelyIncrementLoginCount() throws Exception {
            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk());

            MvcResult result = mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("total_login_count").asInt()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Req2: 入力バリデーション")
    class Req2_InputValidation {

        @Test
        @DisplayName("AC1: UID が 128 文字を超えると入力不備として拒否される")
        void ac1_shouldRejectUidExceeding128Characters() throws Exception {
            String uid129Chars = "A".repeat(129);

            mockMvc.perform(get("/auth/users/{uid}", uid129Chars)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Req3: リソース不存在")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: 存在しない UID で取得するとリソース不在として拒否される")
        void ac1_shouldRejectNonExistentUid() throws Exception {
            mockMvc.perform(get("/auth/users/{uid}", "non-existent-uid")
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"));
        }
    }
}
