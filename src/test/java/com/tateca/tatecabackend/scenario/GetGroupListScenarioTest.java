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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Get Group List — Acceptance Scenario Tests")
class GetGroupListScenarioTest extends AbstractIntegrationTest {

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

    private JsonNode createGroup(String uid, String groupName) throws Exception {
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

    private JsonNode getGroupList(String uid) throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/list")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, uid))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Req1: Get group list (happy path)")
    class Req1_GetGroupList {

        @Test
        @DisplayName("AC1: Should return all groups user belongs to")
        void ac1_shouldReturnAllGroupsUserBelongsTo() throws Exception {
            createGroup(requesterUid, "Group 1");
            createGroup(requesterUid, "Group 2");
            createGroup(requesterUid, "Group 3");

            JsonNode response = getGroupList(requesterUid);
            assertThat(response.path("group_list").size()).isEqualTo(3);
        }

        @Test
        @DisplayName("AC2: Should return empty list when user has no groups")
        void ac2_shouldReturnEmptyListWhenNoGroups() throws Exception {
            JsonNode response = getGroupList(requesterUid);
            assertThat(response.path("group_list").size()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC3: Should not include left groups in the list")
        void ac3_shouldNotIncludeLeftGroups() throws Exception {
            JsonNode groupResponse = createGroup(requesterUid, "Group to Leave");
            String groupId = groupResponse.path("group").path("uuid").asText();
            String hostUuid = null;
            for (JsonNode user : groupResponse.path("users")) {
                if (!user.path("auth_user").isNull() && !user.path("auth_user").isMissingNode()) {
                    hostUuid = user.path("uuid").asText();
                }
            }

            createGroup(requesterUid, "Group to Keep");

            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, hostUuid)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNoContent());

            JsonNode response = getGroupList(requesterUid);
            assertThat(response.path("group_list").size()).isEqualTo(1);
        }
    }
}
