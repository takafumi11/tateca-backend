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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Join Group — Acceptance Scenario Tests")
class JoinGroupScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String creatorUid;
    private String joinerUid;
    private String groupId;
    private String joinToken;
    private String unjoinedMemberUuid;

    @BeforeEach
    void setUp() throws Exception {
        creatorUid = "creator-uid-" + System.nanoTime();
        joinerUid = "joiner-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, creatorUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", creatorUid + "@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, joinerUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", joinerUid + "@example.com"))))
                .andExpect(status().isCreated());

        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, creatorUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", "Test Group",
                                "host_name", "Creator",
                                "participants_name", List.of("MemberToJoin", "OtherMember")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();
        joinToken = groupResponse.path("group").path("join_token").asText();

        for (JsonNode user : groupResponse.path("users")) {
            if ((user.path("auth_user").isNull() || user.path("auth_user").isMissingNode())
                    && unjoinedMemberUuid == null) {
                unjoinedMemberUuid = user.path("uuid").asText();
            }
        }
    }

    private JsonNode createGroupForUid(String uid, String groupName) throws Exception {
        MvcResult result = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, uid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", groupName,
                                "host_name", "Host",
                                "participants_name", List.of("P1")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Req1: Join group (happy path)")
    class Req1_JoinGroup {

        @Test
        @DisplayName("AC1: Should link member to authenticated account")
        void ac1_shouldLinkMemberToAuthenticatedAccount() throws Exception {
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AC2: Should show joined member as authenticated in group info")
        void ac2_shouldShowJoinedMemberAsAuthenticated() throws Exception {
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isOk());

            MvcResult getResult = mockMvc.perform(get("/groups/{groupId}", groupId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode groupInfo = objectMapper.readTree(getResult.getResponse().getContentAsString());
            boolean foundAuthenticated = false;
            for (JsonNode user : groupInfo.path("users")) {
                if (unjoinedMemberUuid.equals(user.path("uuid").asText())) {
                    foundAuthenticated = !user.path("auth_user").isNull() && !user.path("auth_user").isMissingNode();
                }
            }
            assertThat(foundAuthenticated).isTrue();
        }
    }

    @Nested
    @DisplayName("Req2: Duplicate join prevention")
    class Req2_DuplicateJoinPrevention {

        @Test
        @DisplayName("AC1+AC2: Should reject when user already has authenticated membership")
        void ac1ac2_shouldRejectDuplicateMembership() throws Exception {
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isOk());

            String anotherUnjoinedUuid = null;
            MvcResult getResult = mockMvc.perform(get("/groups/{groupId}", groupId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode groupInfo = objectMapper.readTree(getResult.getResponse().getContentAsString());
            for (JsonNode user : groupInfo.path("users")) {
                if ((user.path("auth_user").isNull() || user.path("auth_user").isMissingNode())) {
                    anotherUnjoinedUuid = user.path("uuid").asText();
                }
            }

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", anotherUnjoinedUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("GROUP.ALREADY_JOINED"));
        }
    }

    @Nested
    @DisplayName("Req3: Group participation limit")
    class Req3_GroupParticipationLimit {

        @Test
        @DisplayName("AC1: Should reject when joiner has already joined 9 groups")
        void ac1_shouldRejectWhenJoinerAtGroupLimit() throws Exception {
            IntStream.range(0, 9).forEach(i -> {
                try {
                    createGroupForUid(joinerUid, "Joiner Group " + i);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("USER.MAX_GROUP_COUNT_EXCEEDED"));
        }

        @Test
        @DisplayName("AC2: Should allow privileged user to exceed group limit when joining")
        void ac2_shouldAllowPrivilegedUserToExceedGroupLimit() throws Exception {
            String privilegedUid = "dev-unlimited-uid";

            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, privilegedUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", privilegedUid + "@example.com"))))
                    .andExpect(status().isCreated());

            IntStream.range(0, 9).forEach(i -> {
                try {
                    createGroupForUid(privilegedUid, "PGroup " + i);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, privilegedUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Req4: Join token validation")
    class Req4_JoinTokenValidation {

        @Test
        @DisplayName("AC1: Should reject with invalid join token")
        void ac1_shouldRejectWithInvalidJoinToken() throws Exception {
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", UUID.randomUUID().toString()
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error_code").value("GROUP.INVALID_JOIN_TOKEN"));
        }
    }

    @Nested
    @DisplayName("Req6: Resource not found")
    class Req6_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return 404 for non-existent group")
        void ac1_shouldReturn404ForNonExistentGroup() throws Exception {
            mockMvc.perform(post("/groups/{groupId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP.NOT_FOUND"));
        }

        @Test
        @DisplayName("AC2: Should return 404 for non-existent member")
        void ac2_shouldReturn404ForNonExistentMember() throws Exception {
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, joinerUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", UUID.randomUUID().toString(),
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_FOUND"));
        }

        @Test
        @DisplayName("AC3: Should return 404 when auth user does not exist")
        void ac3_shouldReturn404WhenAuthUserNotFound() throws Exception {
            String unknownUid = "unknown-uid-" + System.nanoTime();

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, unknownUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"));
        }
    }
}
