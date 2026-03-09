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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Leave Group — Acceptance Scenario Tests")
class LeaveGroupScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String creatorUid;
    private String groupId;
    private String joinToken;
    private String creatorUserUuid;
    private String unjoinedMemberUuid;

    @BeforeEach
    void setUp() throws Exception {
        creatorUid = "creator-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, creatorUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", creatorUid + "@example.com"))))
                .andExpect(status().isCreated());

        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, creatorUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", "Test Group",
                                "host_name", "Creator",
                                "participants_name", List.of("Member1")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();
        joinToken = groupResponse.path("group").path("join_token").asText();

        for (JsonNode user : groupResponse.path("users")) {
            if (!user.path("auth_user").isNull() && !user.path("auth_user").isMissingNode()) {
                creatorUserUuid = user.path("uuid").asText();
            } else {
                unjoinedMemberUuid = user.path("uuid").asText();
            }
        }
    }

    @Nested
    @DisplayName("Req1: Leave group (happy path)")
    class Req1_LeaveGroup {

        @Test
        @DisplayName("AC1: Should unlink member from authenticated account")
        void ac1_shouldUnlinkMemberFromAuthenticatedAccount() throws Exception {
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, creatorUserUuid)
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("AC2: Should keep left member in group as unjoined member")
        void ac2_shouldKeepLeftMemberInGroupAsUnjoinedMember() throws Exception {
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, creatorUserUuid)
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isNoContent());

            String anotherUid = "another-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, anotherUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", anotherUid + "@example.com"))))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, anotherUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", unjoinedMemberUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isOk());

            MvcResult getResult = mockMvc.perform(get("/groups/{groupId}", groupId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, anotherUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode groupInfo = objectMapper.readTree(getResult.getResponse().getContentAsString());
            boolean creatorStillInGroup = false;
            for (JsonNode user : groupInfo.path("users")) {
                if (creatorUserUuid.equals(user.path("uuid").asText())) {
                    creatorStillInGroup = true;
                    assertThat(user.path("auth_user").isNull() || user.path("auth_user").isMissingNode()).isTrue();
                }
            }
            assertThat(creatorStillInGroup).isTrue();
        }

        @Test
        @DisplayName("AC3: Should allow rejoin after leaving")
        void ac3_shouldAllowRejoinAfterLeaving() throws Exception {
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, creatorUserUuid)
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, creatorUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "user_uuid", creatorUserUuid,
                                    "join_token", joinToken
                            ))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Req3: Resource not found")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return 404 for non-existent group")
        void ac1_shouldReturn404ForNonExistentGroup() throws Exception {
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", UUID.randomUUID(), creatorUserUuid)
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP.NOT_FOUND"));
        }

        @Test
        @DisplayName("AC2+AC3: Should return 404 for member not in group")
        void ac2ac3_shouldReturn404ForMemberNotInGroup() throws Exception {
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, UUID.randomUUID())
                            .header(X_UID_HEADER, creatorUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_IN_GROUP"));
        }
    }
}
