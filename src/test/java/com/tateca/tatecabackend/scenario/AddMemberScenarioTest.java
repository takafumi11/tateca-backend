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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Add Member — Acceptance Scenario Tests")
class AddMemberScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;

    private String requesterUid;
    private String groupId;

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
                                "group_name", "Test Group",
                                "host_name", "Host",
                                "participants_name", List.of("ExistingMember")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();
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
    @DisplayName("Req1: Add member (happy path)")
    class Req1_AddMember {

        @Test
        @DisplayName("AC1: Should add new unjoined member to group")
        void ac1_shouldAddNewUnjoinedMember() throws Exception {
            mockMvc.perform(post("/groups/{groupId}/members", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "NewMember"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AC2: Should include added member as unjoined in group info")
        void ac2_shouldIncludeAddedMemberAsUnjoinedInGroupInfo() throws Exception {
            mockMvc.perform(post("/groups/{groupId}/members", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "NewMember"))))
                    .andExpect(status().isOk());

            JsonNode groupInfo = getGroupInfo(requesterUid, groupId);
            boolean found = false;
            for (JsonNode user : groupInfo.path("users")) {
                if ("NewMember".equals(user.path("name").asText())) {
                    found = true;
                    assertThat(user.path("auth_user").isNull() || user.path("auth_user").isMissingNode()).isTrue();
                }
            }
            assertThat(found).isTrue();
        }

        @Test
        @DisplayName("AC3: Should increase member count by 1")
        void ac3_shouldIncreaseMemberCountBy1() throws Exception {
            JsonNode before = getGroupInfo(requesterUid, groupId);
            int countBefore = before.path("users").size();

            mockMvc.perform(post("/groups/{groupId}/members", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "NewMember"))))
                    .andExpect(status().isOk());

            JsonNode after = getGroupInfo(requesterUid, groupId);
            assertThat(after.path("users").size()).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("AC4: Should allow adding member with duplicate name")
        void ac4_shouldAllowDuplicateName() throws Exception {
            mockMvc.perform(post("/groups/{groupId}/members", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "ExistingMember"))))
                    .andExpect(status().isOk());

            JsonNode groupInfo = getGroupInfo(requesterUid, groupId);
            long count = 0;
            for (JsonNode user : groupInfo.path("users")) {
                if ("ExistingMember".equals(user.path("name").asText())) {
                    count++;
                }
            }
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Req3: Group size limit")
    class Req3_GroupSizeLimit {

        @Test
        @DisplayName("AC1: Should reject when group has 10 members")
        void ac1_shouldRejectWhenGroupAtMaxSize() throws Exception {
            IntStream.range(0, 8).forEach(i -> {
                try {
                    mockMvc.perform(post("/groups/{groupId}/members", groupId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(X_UID_HEADER, requesterUid)
                                    .content(objectMapper.writeValueAsString(Map.of("member_name", "Member" + i))))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mockMvc.perform(post("/groups/{groupId}/members", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "11thMember"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("GROUP.MAX_SIZE_REACHED"));
        }
    }

    @Nested
    @DisplayName("Req4: Authorization")
    class Req4_Authorization {

        @Test
        @DisplayName("AC1: Should reject when requester is not a group member")
        void ac1_shouldRejectNonGroupMember() throws Exception {
            String outsiderUid = "outsider-uid-" + System.nanoTime();
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, outsiderUid)
                            .content(objectMapper.writeValueAsString(Map.of("email", outsiderUid + "@example.com"))))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/groups/{groupId}/members", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, outsiderUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "NewMember"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_GROUP_MEMBER"));
        }
    }

    @Nested
    @DisplayName("Req5: Resource not found")
    class Req5_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return 404 for non-existent group")
        void ac1_shouldReturn404ForNonExistentGroup() throws Exception {
            mockMvc.perform(post("/groups/{groupId}/members", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid)
                            .content(objectMapper.writeValueAsString(Map.of("member_name", "NewMember"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP.NOT_FOUND"));
        }
    }
}
