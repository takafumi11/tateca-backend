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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Create Group — Acceptance Scenario Tests")
class CreateGroupScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;

    private String requesterUid;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        requesterUid = "scenario-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, requesterUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", requesterUid + "@example.com"))))
                .andExpect(status().isCreated());
    }

    private JsonNode createGroup(String uid, String groupName, String hostName, List<String> participants) throws Exception {
        MvcResult result = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, uid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", groupName,
                                "host_name", hostName,
                                "participants_name", participants
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getGroupInfo(String uid, String groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/{groupId}", groupId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, uid))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Req1: Group creation (happy path)")
    class Req1_GroupCreation {

        @Test
        @DisplayName("AC1: Should create group with unique ID and join token")
        void ac1_shouldCreateGroupWithUniqueIdAndJoinToken() throws Exception {
            JsonNode response = createGroup(requesterUid, "Trip Group", "Host", List.of("Alice", "Bob"));

            assertThat(response.path("group").path("uuid").asText()).isNotEmpty();
            assertThat(response.path("group").path("join_token").asText()).isNotEmpty();
            assertThat(response.path("group").path("name").asText()).isEqualTo("Trip Group");
        }

        @Test
        @DisplayName("AC2: Should include creator as authenticated member")
        void ac2_shouldIncludeCreatorAsAuthenticatedMember() throws Exception {
            JsonNode response = createGroup(requesterUid, "Trip Group", "Host", List.of("Alice"));
            String groupId = response.path("group").path("uuid").asText();

            JsonNode groupInfo = getGroupInfo(requesterUid, groupId);
            JsonNode users = groupInfo.path("users");

            boolean hasAuthenticatedCreator = false;
            for (JsonNode user : users) {
                if (!user.path("auth_user").isNull() && !user.path("auth_user").isMissingNode()) {
                    hasAuthenticatedCreator = true;
                }
            }
            assertThat(hasAuthenticatedCreator).isTrue();
        }

        @Test
        @DisplayName("AC3: Should include participants as unjoined members")
        void ac3_shouldIncludeParticipantsAsUnjoinedMembers() throws Exception {
            JsonNode response = createGroup(requesterUid, "Trip Group", "Host", List.of("Alice", "Bob"));
            String groupId = response.path("group").path("uuid").asText();

            JsonNode groupInfo = getGroupInfo(requesterUid, groupId);
            JsonNode users = groupInfo.path("users");

            long unjoinedCount = 0;
            for (JsonNode user : users) {
                if (user.path("auth_user").isNull() || user.path("auth_user").isMissingNode()) {
                    unjoinedCount++;
                }
            }
            assertThat(unjoinedCount).isEqualTo(2);
        }

        @Test
        @DisplayName("AC4: Should have zero transaction count")
        void ac4_shouldHaveZeroTransactionCount() throws Exception {
            JsonNode response = createGroup(requesterUid, "Trip Group", "Host", List.of("Alice"));

            assertThat(response.path("transaction_count").asLong()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Req3: Group participation limit")
    class Req3_GroupParticipationLimit {

        @Test
        @DisplayName("AC1: Should reject when user has already joined 9 groups")
        void ac1_shouldRejectWhenUserAtGroupLimit() throws Exception {
            IntStream.range(0, 9).forEach(i -> {
                try {
                    createGroup(requesterUid, "Group " + i, "Host", List.of("P" + i));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "group_name", "10th Group",
                                    "host_name", "Host",
                                    "participants_name", List.of("P10")
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("USER.MAX_GROUP_COUNT_EXCEEDED"));
        }

        @Test
        @DisplayName("AC2: Should allow privileged user to exceed group limit")
        void ac2_shouldAllowPrivilegedUserToExceedGroupLimit() throws Exception {
            String privilegedUid = "dev-unlimited-uid";

            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, privilegedUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", privilegedUid + "@example.com"))))
                    .andExpect(status().isCreated());

            IntStream.range(0, 10).forEach(i -> {
                try {
                    createGroup(privilegedUid, "PGroup " + i, "Host", List.of("P" + i));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, privilegedUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "group_name", "11th Group",
                                    "host_name", "Host",
                                    "participants_name", List.of("P11")
                            ))))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Req4: Authentication")
    class Req4_Authentication {

        @Test
        @DisplayName("AC1+AC2: Should reject unauthenticated request")
        void ac1ac2_shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "group_name", "Group",
                                    "host_name", "Host",
                                    "participants_name", List.of("P1")
                            ))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Req5: Resource not found")
    class Req5_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should reject when auth user account does not exist")
        void ac1_shouldRejectWhenAuthUserNotFound() throws Exception {
            String unknownUid = "unknown-uid-" + System.nanoTime();

            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, unknownUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "group_name", "Group",
                                    "host_name", "Host",
                                    "participants_name", List.of("P1")
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"));
        }
    }
}
