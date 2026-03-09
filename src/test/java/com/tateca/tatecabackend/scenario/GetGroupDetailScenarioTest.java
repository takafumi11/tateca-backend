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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Get Group Detail — Acceptance Scenario Tests")
class GetGroupDetailScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String requesterUid;
    private String groupId;

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
                                "participants_name", List.of("Alice", "Bob")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();
    }

    @Nested
    @DisplayName("Req1: Get group detail (happy path)")
    class Req1_GetGroupDetail {

        @Test
        @DisplayName("AC1: Should return group info, member list, and transaction count")
        void ac1_shouldReturnGroupInfoMemberListAndTransactionCount() throws Exception {
            MvcResult result = mockMvc.perform(get("/groups/{groupId}", groupId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

            assertThat(response.path("group").path("uuid").asText()).isEqualTo(groupId);
            assertThat(response.path("group").path("name").asText()).isEqualTo("Test Group");
            assertThat(response.path("users").size()).isEqualTo(3);
            assertThat(response.path("transaction_count").asLong()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Req3: Resource not found")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: Should return 404 for non-existent group")
        void ac1_shouldReturn404ForNonExistentGroup() throws Exception {
            mockMvc.perform(get("/groups/{groupId}", UUID.randomUUID())
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, requesterUid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP.NOT_FOUND"));
        }
    }
}
