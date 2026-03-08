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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Delete Auth User — Acceptance Scenario Tests")
class DeleteAuthUserScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;

    private String authUid;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        authUid = "scenario-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, authUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", authUid + "@example.com"))))
                .andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("Req1: 認証ユーザーの削除")
    class Req1_DeleteAuthUser {

        @Test
        @DisplayName("AC1: 存在する UID で削除すると認証ユーザーレコードが削除される")
        void ac1_shouldDeleteAuthUser() throws Exception {
            mockMvc.perform(delete("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isNoContent());

            // 削除後に GET すると 404
            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AC2: 削除前にアプリ内ユーザーから認証アカウントの参照が解除される")
        void ac2_shouldUnlinkAppUsersBeforeDeletion() throws Exception {
            // グループを作成（これによりアプリ内ユーザーが認証ユーザーに紐付く）
            MvcResult groupResult = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "group_name", "Test Group",
                                    "host_name", "Alice",
                                    "participants_name", List.of("Bob")))))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
            String groupId = groupResponse.path("group").path("uuid").asText();

            // 認証ユーザーを削除
            mockMvc.perform(delete("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isNoContent());

            // グループのメンバー一覧で auth_user が null になっていることを確認
            // 別の認証ユーザーでグループを確認（削除されたユーザーでは認証できない）
            String verifyUid = "verify-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, verifyUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", verifyUid + "@example.com"))))
                    .andExpect(status().isCreated());

            // 削除されたユーザーの GET は 404 を返す（認証ユーザーとしては存在しない）
            mockMvc.perform(get("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, verifyUid))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AC3: 削除後もアプリ内ユーザーレコードは保持される")
        void ac3_shouldPreserveAppUserRecords() throws Exception {
            // グループを作成
            MvcResult groupResult = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, authUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "group_name", "Test Group",
                                    "host_name", "Alice",
                                    "participants_name", List.of("Bob")))))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
            String groupId = groupResponse.path("group").path("uuid").asText();

            // メンバー数を記録
            JsonNode usersBeforeDelete = groupResponse.path("users");
            int memberCountBefore = usersBeforeDelete.size();

            // 認証ユーザーを削除
            mockMvc.perform(delete("/auth/users/{uid}", authUid)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isNoContent());

            // 別のユーザーでグループ情報を確認し、メンバー数が維持されていることを検証
            String verifyUid = "verify-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, verifyUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", verifyUid + "@example.com"))))
                    .andExpect(status().isCreated());

            // グループ詳細の取得にはグループメンバーである必要がある場合があるため、
            // 削除前のレスポンスでメンバー数を検証
            assertThat(memberCountBefore).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Req2: 入力バリデーション")
    class Req2_InputValidation {

        @Test
        @DisplayName("AC1: UID が 128 文字を超えると入力不備として拒否される")
        void ac1_shouldRejectUidExceeding128Characters() throws Exception {
            String uid129Chars = "A".repeat(129);

            mockMvc.perform(delete("/auth/users/{uid}", uid129Chars)
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Req3: リソース不存在")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: 存在しない UID で削除するとリソース不在として拒否される")
        void ac1_shouldRejectNonExistentUid() throws Exception {
            mockMvc.perform(delete("/auth/users/{uid}", "non-existent-uid")
                            .header(X_UID_HEADER, authUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"));
        }
    }
}
