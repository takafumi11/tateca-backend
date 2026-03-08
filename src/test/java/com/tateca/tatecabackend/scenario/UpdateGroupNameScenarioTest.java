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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Update Group Name — Acceptance Scenario Tests")
class UpdateGroupNameScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;

    private String requesterUid;
    private String groupId;
    private String originalJoinToken;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
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
                                "group_name", "Original Name",
                                "host_name", "Host",
                                "participants_name", List.of("Alice")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();
        originalJoinToken = groupResponse.path("group").path("join_token").asText();
    }

    private JsonNode getGroupInfo(String uid, String gId) throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/{groupId}", gId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, uid))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Req1: Update group name (happy path)")
    class Req1_UpdateGroupName {

        @Test
        @DisplayName("AC1: Should update group name and return updated info")
        void ac1_shouldUpdateGroupNameAndReturnUpdatedInfo() throws Exception {
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("group_name", "Updated Name"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Updated Name"));
        }

        @Test
        @DisplayName("AC2: Should reflect updated name in group info")
        void ac2_shouldReflectUpdatedNameInGroupInfo() throws Exception {
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("group_name", "New Name"))))
                    .andExpect(status().isOk());

            JsonNode groupInfo = getGroupInfo(requesterUid, groupId);
            assertThat(groupInfo.path("group").path("name").asText()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("AC3: Should not change other attributes (join token, created_at)")
        void ac3_shouldNotChangeOtherAttributes() throws Exception {
            JsonNode beforeUpdate = getGroupInfo(requesterUid, groupId);
            String createdAtBefore = beforeUpdate.path("group").path("created_at").asText();

            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("group_name", "Changed Name"))))
                    .andExpect(status().isOk());

            JsonNode afterUpdate = getGroupInfo(requesterUid, groupId);
            assertThat(afterUpdate.path("group").path("join_token").asText()).isEqualTo(originalJoinToken);
            assertThat(afterUpdate.path("group").path("created_at").asText()).isEqualTo(createdAtBefore);
        }

        @Test
        @DisplayName("AC4: Should update the updated_at timestamp")
        void ac4_shouldUpdateUpdatedAtTimestamp() throws Exception {
            JsonNode beforeUpdate = getGroupInfo(requesterUid, groupId);
            String updatedAtBefore = beforeUpdate.path("group").path("updated_at").asText();

            Thread.sleep(1100);

            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("group_name", "Newer Name"))))
                    .andExpect(status().isOk());

            JsonNode afterUpdate = getGroupInfo(requesterUid, groupId);
            String updatedAtAfter = afterUpdate.path("group").path("updated_at").asText();
            assertThat(updatedAtAfter).isNotEqualTo(updatedAtBefore);
        }
    }

    @Nested
    @DisplayName("Req3: Resource not found")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return 404 for non-existent group")
        void ac1_shouldReturn404ForNonExistentGroup() throws Exception {
            mockMvc.perform(patch("/groups/{groupId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("group_name", "New Name"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP.NOT_FOUND"));
        }
    }
}
