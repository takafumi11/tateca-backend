package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateGroupNameRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.dto.response.internal.GroupResponse;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("GroupController Web Tests")
class GroupControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupService groupService;

    private static final String BASE_ENDPOINT = "/groups";

    // ========================================
    // createGroup Tests
    // ========================================

    @Nested
    @DisplayName("POST /groups - Create Group")
    class CreateGroupTests {

        @Test
        @DisplayName("Should return 201 CREATED when group is created successfully")
        void shouldReturn201WhenGroupCreated() throws Exception {
            // Given: Valid request
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group",
                    "Host User",
                    List.of("User 1", "User 2")
            );

            UUID groupId = UUID.randomUUID();
            GroupResponse groupInfo = new GroupResponse(
                    groupId.toString(),
                    "New Group",
                    UUID.randomUUID().toString(),
                    "2024-12-31T23:59:59Z",
                    "2024-01-01T09:00:00+09:00",
                    "2024-01-01T09:00:00+09:00"
            );

            GroupResponseDTO expectedResponse = new GroupResponseDTO(groupInfo, List.of(), 0L);

            when(groupService.createGroup(anyString(), any(CreateGroupRequestDTO.class)))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 201
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.group").exists())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.transaction_count").value(0));

            verify(groupService, times(1)).createGroup(anyString(), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when user exceeds group limit")
        void shouldReturn409WhenGroupLimitExceeded() throws Exception {
            // Given: Service throws CONFLICT exception
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group",
                    "Host",
                    List.of("Participant")
            );

            when(groupService.createGroup(anyString(), any(CreateGroupRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "can't join more than 10 groups"));

            // When & Then: Should return 409
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").exists());

            verify(groupService, times(1)).createGroup(anyString(), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyMissing() throws Exception {
            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when participants list is empty")
        void shouldReturn400WhenParticipantsListIsEmpty() throws Exception {
            // Given: Request with empty participants list
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group",
                    "Host User",
                    List.of()  // Empty list violates @Size(min = 1)
            );

            // When & Then: Should return 400 with validation error
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when group groupName is blank")
        void shouldReturn400WhenGroupNameIsBlank() throws Exception {
            // Given: Request with blank group groupName
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"group_name\":\"\",\"host_name\":\"Host\",\"participants_name\":[\"P1\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when group groupName exceeds 100 characters")
        void shouldReturn400WhenGroupNameExceedsMaxLength() throws Exception {
            // Given: Group groupName with 101 characters
            String longName = "A".repeat(101);
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    longName,
                    "Host",
                    List.of("P1")
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when host groupName is blank")
        void shouldReturn400WhenHostNameIsBlank() throws Exception {
            // Given: Request with blank host groupName
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"group_name\":\"Group\",\"host_name\":\"\",\"participants_name\":[\"P1\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when host groupName exceeds 50 characters")
        void shouldReturn400WhenHostNameExceedsMaxLength() throws Exception {
            // Given: Host groupName with 51 characters
            String longName = "A".repeat(51);
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "Group",
                    longName,
                    List.of("P1")
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when participants list is null")
        void shouldReturn400WhenParticipantsListIsNull() throws Exception {
            // Given: Request with null participants
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"group_name\":\"Group\",\"host_name\":\"Host\",\"participants_name\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when participants list exceeds 8 members")
        void shouldReturn400WhenParticipantsListExceedsMaxSize() throws Exception {
            // Given: Request with 9 participants (exceeds max of 8)
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "Group",
                    "Host",
                    List.of("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9")
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when participant groupName is blank")
        void shouldReturn400WhenParticipantNameIsBlank() throws Exception {
            // Given: Request with blank participant groupName
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"group_name\":\"Group\",\"host_name\":\"Host\",\"participants_name\":[\"\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 400 when participant groupName exceeds 50 characters")
        void shouldReturn400WhenParticipantNameExceedsMaxLength() throws Exception {
            // Given: Participant groupName with 51 characters
            String longName = "A".repeat(51);
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "Group",
                    "Host",
                    List.of(longName)
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is missing")
        void shouldReturn415WhenContentTypeIsMissing() throws Exception {
            // Given: Valid JSON but no Content-Type header
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group",
                    "Host User",
                    List.of("User 1")
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(groupService, never()).createGroup(anyString(), any());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is not JSON")
        void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
            // Given: Valid JSON but wrong Content-Type
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group",
                    "Host User",
                    List.of("User 1")
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(groupService, never()).createGroup(anyString(), any());
        }
    }

    // ========================================
    // updateGroupName Tests
    // ========================================

    @Nested
    @DisplayName("PATCH /groups/{groupId} - Update Group Name")
    class UpdateGroupNameTests {

        @Test
        @DisplayName("Should return 200 OK when group groupName is updated")
        void shouldReturn200WhenGroupNameUpdated() throws Exception {
            // Given: Valid request
            UUID groupId = UUID.randomUUID();
            UpdateGroupNameRequestDTO request = new UpdateGroupNameRequestDTO("Updated Name");

            GroupResponse groupInfo = new GroupResponse(
                    groupId.toString(),
                    "Updated Name",
                    UUID.randomUUID().toString(),
                    "2024-12-31T23:59:59Z",
                    "2024-01-01T09:00:00+09:00",
                    "2024-01-01T09:00:00+09:00"
            );

            GroupResponseDTO expectedResponse = new GroupResponseDTO(groupInfo, List.of(), 0L);

            when(groupService.updateGroupName(eq(groupId), anyString()))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.group.name").value("Updated Name"));

            verify(groupService, times(1)).updateGroupName(eq(groupId), anyString());
        }

        @Test
        @DisplayName("Should return 404 when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given: Service throws NOT_FOUND exception
            UUID groupId = UUID.randomUUID();
            UpdateGroupNameRequestDTO request = new UpdateGroupNameRequestDTO("New Name");

            when(groupService.updateGroupName(eq(groupId), anyString()))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "group not found"));

            // When & Then: Should return 404
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());

            verify(groupService, times(1)).updateGroupName(eq(groupId), anyString());
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenInvalidUUID() throws Exception {
            // Given: Invalid UUID
            String invalidUUID = "not-a-uuid";
            UpdateGroupNameRequestDTO request = new UpdateGroupNameRequestDTO("New Name");

            // When & Then: Should return 400
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", invalidUUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(groupService, never()).updateGroupName(any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when group name is blank")
        void shouldReturn400WhenGroupNameIsBlank() throws Exception {
            // Given: Request with blank group name
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"group_name\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).updateGroupName(any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when group name exceeds 100 characters")
        void shouldReturn400WhenGroupNameExceedsMaxLength() throws Exception {
            // Given: Group name with 101 characters
            UUID groupId = UUID.randomUUID();
            String longName = "A".repeat(101);
            UpdateGroupNameRequestDTO request = new UpdateGroupNameRequestDTO(longName);

            // When & Then: Should return 400
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).updateGroupName(any(), anyString());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is missing")
        void shouldReturn415WhenContentTypeIsMissing() throws Exception {
            // Given: Valid JSON but no Content-Type header
            UUID groupId = UUID.randomUUID();
            UpdateGroupNameRequestDTO request = new UpdateGroupNameRequestDTO("Updated Name");

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", groupId)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(groupService, never()).updateGroupName(any(), anyString());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is not JSON")
        void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
            // Given: Valid JSON but wrong Content-Type
            UUID groupId = UUID.randomUUID();
            UpdateGroupNameRequestDTO request = new UpdateGroupNameRequestDTO("Updated Name");

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(patch(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(groupService, never()).updateGroupName(any(), anyString());
        }
    }

    // ========================================
    // getGroupInfo Tests
    // ========================================

    @Nested
    @DisplayName("GET /groups/{groupId} - Get Group Info")
    class GetGroupInfoTests {

        @Test
        @DisplayName("Should return 200 OK with group information")
        void shouldReturn200WithGroupInfo() throws Exception {
            // Given: Group exists
            UUID groupId = UUID.randomUUID();

            GroupResponse groupInfo = new GroupResponse(
                    groupId.toString(),
                    "Test Group",
                    UUID.randomUUID().toString(),
                    "2024-12-31T23:59:59Z",
                    "2024-01-01T09:00:00+09:00",
                    "2024-01-01T09:00:00+09:00"
            );

            GroupResponseDTO expectedResponse = new GroupResponseDTO(groupInfo, List.of(), 5L);

            when(groupService.getGroupInfo(eq(groupId)))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(get(BASE_ENDPOINT + "/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()))
                    .andExpect(jsonPath("$.transaction_count").value(5));

            verify(groupService, times(1)).getGroupInfo(eq(groupId));
        }

        @Test
        @DisplayName("Should return 404 when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given: Group does not exist
            UUID groupId = UUID.randomUUID();

            when(groupService.getGroupInfo(eq(groupId)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found"));

            // When & Then: Should return 404
            mockMvc.perform(get(BASE_ENDPOINT + "/{groupId}", groupId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(groupService, times(1)).getGroupInfo(eq(groupId));
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenInvalidUUID() throws Exception {
            // Given: Invalid UUID
            String invalidUUID = "not-a-uuid";

            // When & Then: Should return 400
            mockMvc.perform(get(BASE_ENDPOINT + "/{groupId}", invalidUUID))
                    .andExpect(status().isBadRequest());

            verify(groupService, never()).getGroupInfo(any());
        }
    }

    // ========================================
    // getGroupList Tests
    // ========================================

    @Nested
    @DisplayName("GET /groups/list - Get Group List")
    class GetGroupListTests {

        @Test
        @DisplayName("Should return 200 OK with group list")
        void shouldReturn200WithGroupList() throws Exception {
            // Given: User has groups
            GroupListResponseDTO expectedResponse = new GroupListResponseDTO(List.of());

            when(groupService.getGroupList(anyString()))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(get(BASE_ENDPOINT + "/list"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.group_list").isArray());

            verify(groupService, times(1)).getGroupList(anyString());
        }

        @Test
        @DisplayName("Should return empty list when user has no groups")
        void shouldReturnEmptyListWhenNoGroups() throws Exception {
            // Given: User has no groups
            GroupListResponseDTO expectedResponse = new GroupListResponseDTO(List.of());

            when(groupService.getGroupList(anyString()))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200 with empty list
            mockMvc.perform(get(BASE_ENDPOINT + "/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list").isEmpty());

            verify(groupService, times(1)).getGroupList(anyString());
        }
    }

    // ========================================
    // joinGroupInvited Tests
    // ========================================

    @Nested
    @DisplayName("POST /groups/{groupId} - Join Group")
    class JoinGroupInvitedTests {

        @Test
        @DisplayName("Should return 200 OK when user joins group successfully")
        void shouldReturn200WhenUserJoinsGroup() throws Exception {
            // Given: Valid join request
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();

            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

            GroupResponse groupInfo = new GroupResponse(
                    groupId.toString(),
                    "Test Group",
                    joinToken.toString(),
                    "2024-12-31T23:59:59Z",
                    "2024-01-01T09:00:00+09:00",
                    "2024-01-01T09:00:00+09:00"
            );

            GroupResponseDTO expectedResponse = new GroupResponseDTO(groupInfo, List.of(), 0L);

            when(groupService.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), anyString()))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.group").exists());

            verify(groupService, times(1)).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), anyString());
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when join token is invalid")
        void shouldReturn403WhenInvalidJoinToken() throws Exception {
            // Given: Invalid join token
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();

            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

            when(groupService.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), anyString()))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid join token"));

            // When & Then: Should return 403
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verify(groupService, times(1)).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), anyString());
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when user already in group")
        void shouldReturn409WhenAlreadyInGroup() throws Exception {
            // Given: User already in group
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();

            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

            when(groupService.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), anyString()))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already joined this group"));

            // When & Then: Should return 409
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));

            verify(groupService, times(1)).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), anyString());
        }

        @Test
        @DisplayName("Should return 400 when user UUID is null")
        void shouldReturn400WhenUserUuidIsNull() throws Exception {
            // Given: Request with null user UUID
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_uuid\":null,\"join_token\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when join token is null")
        void shouldReturn400WhenJoinTokenIsNull() throws Exception {
            // Given: Request with null join token
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_uuid\":\"" + UUID.randomUUID() + "\",\"join_token\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when user UUID has invalid format (boolean)")
        void shouldReturn400WhenUserUuidIsBoolean() throws Exception {
            // Given: Request with boolean instead of UUID (Jackson deserialization error)
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_uuid\":true,\"join_token\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid value for field 'user_uuid': expected UUID but received String"));

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when user UUID has invalid format (string)")
        void shouldReturn400WhenUserUuidIsInvalidString() throws Exception {
            // Given: Request with invalid UUID string (Jackson deserialization error)
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_uuid\":\"invalid-uuid\",\"join_token\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid value for field 'user_uuid': expected UUID but received String"));

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenGroupIdIsInvalidUUID() throws Exception {
            // Given: Invalid UUID in path variable
            String invalidGroupId = "not-a-uuid";
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();

            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", invalidGroupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is missing")
        void shouldReturn415WhenContentTypeIsMissing() throws Exception {
            // Given: Valid JSON but no Content-Type header
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is not JSON")
        void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
            // Given: Valid JSON but wrong Content-Type
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT + "/{groupId}", groupId)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(groupService, never()).joinGroupInvited(any(), any(), anyString());
        }
    }

    // ========================================
    // leaveGroup Tests
    // ========================================

    @Nested
    @DisplayName("DELETE /groups/{groupId}/users/{userUuid} - Leave Group")
    class LeaveGroupTests {

        @Test
        @DisplayName("Should return 204 NO CONTENT when user leaves group")
        void shouldReturn204WhenUserLeavesGroup() throws Exception {
            // Given: User is in group
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doNothing().when(groupService).leaveGroup(eq(groupId), eq(userUuid));

            // When & Then: Should return 204
            mockMvc.perform(delete(BASE_ENDPOINT + "/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNoContent());

            verify(groupService, times(1)).leaveGroup(eq(groupId), eq(userUuid));
        }

        @Test
        @DisplayName("Should return 404 when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given: Group does not exist
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"))
                    .when(groupService).leaveGroup(eq(groupId), eq(userUuid));

            // When & Then: Should return 404
            mockMvc.perform(delete(BASE_ENDPOINT + "/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(groupService, times(1)).leaveGroup(eq(groupId), eq(userUuid));
        }

        @Test
        @DisplayName("Should return 400 when groupId UUID format is invalid")
        void shouldReturn400WhenInvalidGroupIdUUID() throws Exception {
            // Given: Invalid groupId UUID
            String invalidGroupId = "not-a-uuid";
            UUID userUuid = UUID.randomUUID();

            // When & Then: Should return 400
            mockMvc.perform(delete(BASE_ENDPOINT + "/{groupId}/users/{userUuid}", invalidGroupId, userUuid))
                    .andExpect(status().isBadRequest());

            verify(groupService, never()).leaveGroup(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when userUuid UUID format is invalid")
        void shouldReturn400WhenInvalidUserUuidUUID() throws Exception {
            // Given: Invalid userUuid UUID
            UUID groupId = UUID.randomUUID();
            String invalidUserUuid = "not-a-uuid";

            // When & Then: Should return 400
            mockMvc.perform(delete(BASE_ENDPOINT + "/{groupId}/users/{userUuid}", groupId, invalidUserUuid))
                    .andExpect(status().isBadRequest());

            verify(groupService, never()).leaveGroup(any(), any());
        }
    }
}
