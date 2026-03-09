package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.CurrencyRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance scenario tests for Remove Member.
 *
 * <p>Each test maps directly to an Acceptance Criterion in
 * {@code docs/specs/remove-member/requirements.md}.
 *
 * <p>All setup and verification is performed exclusively through HTTP endpoints,
 * mirroring the actual client flow. No direct repository or database access is used.
 *
 * <p>Client flow:
 * <ol>
 *   <li>POST /auth/users — create authenticated user</li>
 *   <li>POST /groups — create group (host + placeholder members)</li>
 *   <li>DELETE /groups/{groupId}/members/{userUuid} — remove member (test target)</li>
 *   <li>GET /groups/{groupId} — verify member removal via group detail response</li>
 * </ol>
 */
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Remove Member — Acceptance Scenario Tests")
class RemoveMemberScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";
    private static final String REMOVE_MEMBER_ENDPOINT = "/groups/{groupId}/members/{userUuid}";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;

    private String requesterUid;
    private String groupId;
    private String joinToken;
    private String requesterUserUuid;
    private String unjoinedMemberUuid;

    @BeforeEach
    void setUp() throws Exception {
        requesterUid = "scenario-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, requesterUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", requesterUid + "@example.com"))))
                .andExpect(status().isCreated());

        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, requesterUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", "Test Group",
                                "host_name", "Host",
                                "participants_name", List.of("UnjoinedMember", "AnotherMember")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();
        joinToken = groupResponse.path("group").path("join_token").asText();

        JsonNode users = groupResponse.path("users");
        for (JsonNode user : users) {
            if (!user.path("auth_user").isNull() && !user.path("auth_user").isMissingNode()) {
                requesterUserUuid = user.path("uuid").asText();
            } else if (unjoinedMemberUuid == null) {
                unjoinedMemberUuid = user.path("uuid").asText();
            }
        }
    }

    private JsonNode getGroupInfoViaApi(String uid) throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/{groupId}", groupId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, uid))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private int getMemberCountViaApi(String uid) throws Exception {
        return getGroupInfoViaApi(uid).path("users").size();
    }

    private boolean isMemberInGroup(String uid, String targetUserUuid) throws Exception {
        JsonNode users = getGroupInfoViaApi(uid).path("users");
        for (JsonNode user : users) {
            if (targetUserUuid.equals(user.path("uuid").asText())) {
                return true;
            }
        }
        return false;
    }

    private String findUnjoinedMemberUuid(JsonNode groupResponse, String excludeUuid) {
        for (JsonNode user : groupResponse.path("users")) {
            if ((user.path("auth_user").isNull() || user.path("auth_user").isMissingNode())
                    && !user.path("uuid").asText().equals(excludeUuid)) {
                return user.path("uuid").asText();
            }
        }
        throw new AssertionError("No unjoined member found (excluding " + excludeUuid + ")");
    }

    private String createAuthUserAndJoinGroup(String memberUuid) throws Exception {
        String joinerUid = "joiner-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, joinerUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", joinerUid + "@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/groups/{groupId}", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, joinerUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "user_uuid", memberUuid,
                                "join_token", joinToken
                        ))))
                .andExpect(status().isOk());

        return joinerUid;
    }

    private void createTransactionWithPayer(String payerUuid, String obligationUserUuid) throws Exception {
        mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, requesterUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", "Test Transaction",
                                "amount", 1000,
                                "currency_code", "JPY",
                                "date_str", "2025-01-15T12:00:00+09:00",
                                "payer_id", payerUuid,
                                "loan", Map.of(
                                        "obligations", List.of(
                                                Map.of("amount", 1000, "user_uuid", obligationUserUuid)
                                        )
                                )
                        ))))
                .andExpect(status().isCreated());
    }

    // =========================================================
    // Req 1: 未参加メンバーの削除
    // =========================================================

    @Nested
    @DisplayName("Req1: Removing an unjoined member")
    class Req1_RemoveUnjoinedMember {

        @Test
        @DisplayName("AC1: Should completely remove the unjoined member from the group")
        void ac1_shouldRemoveUnjoinedMember() throws Exception {
            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("AC2: Should not include the deleted member in group info")
        void ac2_shouldNotIncludeDeletedMemberInGroupInfo() throws Exception {
            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNoContent());

            assertThat(isMemberInGroup(requesterUid, unjoinedMemberUuid)).isFalse();
        }

        @Test
        @DisplayName("AC3: Should decrease the group member count by 1")
        void ac3_shouldDecreaseMemberCount() throws Exception {
            int countBefore = getMemberCountViaApi(requesterUid);

            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNoContent());

            int countAfter = getMemberCountViaApi(requesterUid);
            assertThat(countAfter).isEqualTo(countBefore - 1);
        }
    }

    // =========================================================
    // Req 2: 削除対象の制限
    // =========================================================

    @Nested
    @DisplayName("Req2: Restriction on removal targets")
    class Req2_RestrictionOnRemovalTargets {

        @Test
        @DisplayName("AC1: Should reject removal of a joined member")
        void ac1_shouldRejectRemovalOfJoinedMember() throws Exception {
            String anotherUnjoinedUuid = findUnjoinedMemberUuid(
                    getGroupInfoViaApi(requesterUid), unjoinedMemberUuid);
            createAuthUserAndJoinGroup(anotherUnjoinedUuid);

            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, anotherUnjoinedUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("MEMBER.ALREADY_JOINED"));
        }

        @Test
        @DisplayName("AC2: Should reject self-removal (requester is a joined member)")
        void ac2_shouldRejectSelfRemoval() throws Exception {
            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, requesterUserUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("MEMBER.ALREADY_JOINED"));
        }
    }

    // =========================================================
    // Req 3: 取引関与メンバーの削除制限
    // =========================================================

    @Nested
    @DisplayName("Req3: Transaction involvement restriction")
    class Req3_TransactionInvolvementRestriction {

        @BeforeEach
        void setUpExchangeRate() {
            currencyRepository.save(TestFixtures.Currencies.jpy());
            LocalDate txDate = LocalDate.of(2025, 1, 15);
            var currency = currencyRepository.findById("JPY").orElseThrow();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("JPY")
                    .date(txDate)
                    .exchangeRate(BigDecimal.ONE)
                    .currency(currency)
                    .build());
        }

        @Test
        @DisplayName("AC1: Should reject removal of unjoined member recorded as a payer")
        void ac1_shouldRejectRemovalOfPayer() throws Exception {
            createTransactionWithPayer(unjoinedMemberUuid, requesterUserUuid);

            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("MEMBER.HAS_TRANSACTIONS"));
        }

        @Test
        @DisplayName("AC2: Should reject removal of unjoined member recorded as an obligor")
        void ac2_shouldRejectRemovalOfObligor() throws Exception {
            createTransactionWithPayer(requesterUserUuid, unjoinedMemberUuid);

            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("MEMBER.HAS_TRANSACTIONS"));
        }
    }

    // =========================================================
    // Req 4: 認証と認可
    // =========================================================

    @Nested
    @DisplayName("Req4: Authentication and authorization")
    class Req4_AuthenticationAndAuthorization {

        @Test
        @DisplayName("AC1+AC2: Should reject unauthenticated request with 401")
        void ac1ac2_shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value(anyOf(
                            is("AUTH.MISSING_CREDENTIALS"),
                            is("AUTH.INVALID_FORMAT"),
                            is("AUTH.INVALID_TOKEN")
                    )));
        }

        @Test
        @DisplayName("AC3: Should reject when requester is not a group member")
        void ac3_shouldRejectNonGroupMember() throws Exception {
            String outsiderUid = "outsider-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, outsiderUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", outsiderUid + "@example.com"))))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, unjoinedMemberUuid)
                            .header(X_UID_HEADER, outsiderUid))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_GROUP_MEMBER"));

            assertThat(isMemberInGroup(requesterUid, unjoinedMemberUuid)).isTrue();
        }
    }

    // =========================================================
    // Req 5: リソース不存在
    // =========================================================

    @Nested
    @DisplayName("Req5: Resource not found")
    class Req5_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return 404 for non-existent group")
        void ac1_shouldReturn404ForNonExistentGroup() throws Exception {
            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, UUID.randomUUID(), unjoinedMemberUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP.NOT_FOUND"));
        }

        @Test
        @DisplayName("AC2: Should return 404 for non-existent member in the group")
        void ac2_shouldReturn404ForNonExistentMember() throws Exception {
            mockMvc.perform(delete(REMOVE_MEMBER_ENDPOINT, groupId, UUID.randomUUID())
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_FOUND"));
        }
    }
}
