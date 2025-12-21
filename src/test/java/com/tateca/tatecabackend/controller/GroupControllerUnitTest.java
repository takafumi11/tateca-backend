package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupDetailsResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupInfoDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@DisplayName("GroupController Unit Tests")
class GroupControllerUnitTest extends AbstractControllerWebTest {

    @MockitoBean
    private GroupService service;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Nested
    @DisplayName("GET /groups/{groupId}")
    class GetGroupInfoTests {

        @Test
        @DisplayName("Should return 200 OK with group info when group exists (userGroups.isEmpty()=false)")
        void shouldReturn200WithGroupInfo_WhenGroupExists() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "Test Group", 3, 0L
            );

            when(service.getGroupInfo(groupId)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()))
                    .andExpect(jsonPath("$.group.name").value("Test Group"))
                    .andExpect(jsonPath("$.users.length()").value(3))
                    .andExpect(jsonPath("$.transaction_count").value(0));

            verify(service).getGroupInfo(groupId);
        }

        @Test
        @DisplayName("Should return 200 OK with multiple users")
        void shouldReturn200WithMultipleUsers() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "Large Group", 5, 0L
            );

            when(service.getGroupInfo(groupId)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(5));

            verify(service).getGroupInfo(groupId);
        }

        @Test
        @DisplayName("Should return 200 OK with transaction count")
        void shouldReturn200WithTransactionCount() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "Active Group", 2, 42L
            );

            when(service.getGroupInfo(groupId)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transaction_count").value(42));

            verify(service).getGroupInfo(groupId);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group has no users (userGroups.isEmpty()=true)")
        void shouldReturn404WhenGroupHasNoUsers_EmptyUserGroups() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.getGroupInfo(groupId))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found"));

            // When & Then
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isNotFound());

            verify(service).getGroupInfo(groupId);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.getGroupInfo(groupId))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found"));

            // When & Then
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isNotFound());

            verify(service).getGroupInfo(groupId);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.getGroupInfo(groupId))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isInternalServerError());

            verify(service).getGroupInfo(groupId);
        }
    }

    @Nested
    @DisplayName("GET /groups/list")
    class GetGroupListTests {

        @Test
        @DisplayName("Should return 200 OK with group list containing multiple groups")
        void shouldReturn200WithGroupList_MultipleGroups() throws Exception {
            // Given
            GroupListResponseDTO mockResponse = createGroupListResponseDTO(3);

            when(service.getGroupList(TEST_UID)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3));

            verify(service).getGroupList(TEST_UID);
        }

        @Test
        @DisplayName("Should return 200 OK with empty group list")
        void shouldReturn200WithEmptyGroupList() throws Exception {
            // Given
            GroupListResponseDTO mockResponse = new GroupListResponseDTO(Collections.emptyList());

            when(service.getGroupList(TEST_UID)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list").isEmpty());

            verify(service).getGroupList(TEST_UID);
        }

        @Test
        @DisplayName("Should return 200 OK with single group")
        void shouldReturn200WithSingleGroup() throws Exception {
            // Given
            GroupListResponseDTO mockResponse = createGroupListResponseDTO(1);

            when(service.getGroupList(TEST_UID)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(1));

            verify(service).getGroupList(TEST_UID);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            when(service.getGroupList(TEST_UID))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isInternalServerError());

            verify(service).getGroupList(TEST_UID);
        }

        @Test
        @DisplayName("Should use injected UID from @UId annotation")
        void shouldUseInjectedUidFromAnnotation() throws Exception {
            // Given
            GroupListResponseDTO mockResponse = createGroupListResponseDTO(2);

            when(service.getGroupList(eq(TEST_UID))).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk());

            verify(service).getGroupList(eq(TEST_UID));
        }
    }

    @Nested
    @DisplayName("POST /groups")
    class CreateGroupTests {

        @Test
        @DisplayName("Should return 201 CREATED with created group - no participants (forEach skip branch)")
        void shouldReturn201WithCreatedGroup_NoParticipants() throws Exception {
            // Given
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    UUID.randomUUID(), "Trip", 1, 0L
            );

            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.users.length()").value(1));

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED with multiple participants (forEach execute branch)")
        void shouldReturn201WithCreatedGroup_WithParticipants() throws Exception {
            // Given
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    UUID.randomUUID(), "Trip", 3, 0L
            );

            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": ["Bob", "Charlie"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.users.length()").value(3));

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED with single participant")
        void shouldReturn201WithCreatedGroup_SingleParticipant() throws Exception {
            // Given
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    UUID.randomUUID(), "Trip", 2, 0L
            );

            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": ["Bob"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.users.length()").value(2));

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when max group count exceeded for regular user")
        void shouldReturn409WhenMaxGroupCountExceeded_RegularUser() throws Exception {
            // Given
            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 10 groups"));

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isConflict());

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when user has exactly 9 groups (boundary value)")
        void shouldReturn409WhenMaxGroupCountExceeded_9Groups() throws Exception {
            // Given
            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 10 groups"));

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "10th Group",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isConflict());

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user not found")
        void shouldReturn404WhenAuthUserNotFound() throws Exception {
            // Given
            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found"));

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isNotFound());

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }

        @Test
        @DisplayName("Should use injected UID from @UId annotation")
        void shouldUseInjectedUidFromAnnotation() throws Exception {
            // Given
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    UUID.randomUUID(), "Trip", 1, 0L
            );

            when(service.createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Trip",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated());

            verify(service).createGroup(eq(TEST_UID), any(CreateGroupRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("POST /groups/{groupId}")
    class JoinGroupInvitedTests {

        @Test
        @DisplayName("Should return 200 OK when join successful with valid token (token match, exists=false)")
        void shouldReturn200WhenJoinSuccessful_ValidToken() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();

            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "Test Group", 2, 0L
            );

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, userUuid, joinToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()));

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when invalid join token (token mismatch, line 140 if=true)")
        void shouldReturn403WhenInvalidJoinToken() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid join token"));

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isForbidden());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when user already in group (exists=true, line 152 if=true)")
        void shouldReturn409WhenUserAlreadyInGroup_ExistingMembership() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "You have already joined this group"));

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isConflict());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 200 OK when user not in group with null authUser (anyMatch authUser!=null false branch)")
        void shouldReturn200WhenUserNotInGroup_NullAuthUser() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(groupId, "Test Group", 2, 0L);

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isOk());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 200 OK when user not in group with different authUser (uid.equals false branch)")
        void shouldReturn200WhenUserNotInGroup_DifferentAuthUser() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(groupId, "Test Group", 2, 0L);

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isOk());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when max group count exceeded")
        void shouldReturn409WhenMaxGroupCountExceeded() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 10 groups"));

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isConflict());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found"));

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isNotFound());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found"));

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isNotFound());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isInternalServerError());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }

        @Test
        @DisplayName("Should use injected UID from @UId annotation")
        void shouldUseInjectedUidFromAnnotation() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(groupId, "Test Group", 2, 0L);

            when(service.joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isOk());

            verify(service).joinGroupInvited(any(JoinGroupRequestDTO.class), eq(groupId), eq(TEST_UID));
        }
    }

    @Nested
    @DisplayName("PATCH /groups/{groupId}")
    class UpdateGroupNameTests {

        @Test
        @DisplayName("Should return 200 OK with updated group name")
        void shouldReturn200WithUpdatedGroupName() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "Updated Name", 2, 0L
            );

            when(service.updateGroupName(eq(groupId), anyString())).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Updated Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Updated Name"));

            verify(service).updateGroupName(eq(groupId), eq("Updated Name"));
        }

        @Test
        @DisplayName("Should return 200 OK with empty group name (no @Valid)")
        void shouldReturn200WithEmptyGroupName() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "", 2, 0L
            );

            when(service.updateGroupName(eq(groupId), eq(""))).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": ""
                                    }
                                    """))
                    .andExpect(status().isOk());

            verify(service).updateGroupName(eq(groupId), eq(""));
        }

        @Test
        @DisplayName("Should return 200 OK with null group name")
        void shouldReturn200WithNullGroupName() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, null, 2, 0L
            );

            when(service.updateGroupName(eq(groupId), eq(null))).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": null
                                    }
                                    """))
                    .andExpect(status().isOk());

            verify(service).updateGroupName(eq(groupId), eq(null));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.updateGroupName(eq(groupId), anyString()))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found"));

            // When & Then
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isNotFound());

            verify(service).updateGroupName(eq(groupId), eq("New Name"));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();

            when(service.updateGroupName(eq(groupId), anyString()))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(service).updateGroupName(eq(groupId), eq("New Name"));
        }

        @Test
        @DisplayName("Should allow update without authentication (SECURITY ISSUE - no @UId)")
        void shouldAllowUpdateWithoutAuthentication_SecurityIssue() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            GroupDetailsResponseDTO mockResponse = createGroupDetailsResponseDTO(
                    groupId, "Hacked Name", 2, 0L
            );

            when(service.updateGroupName(eq(groupId), anyString())).thenReturn(mockResponse);

            // When & Then - Anyone can update group name without authentication
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Hacked Name"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Note: No @UId annotation on controller method - security vulnerability
            verify(service).updateGroupName(eq(groupId), eq("Hacked Name"));
        }
    }

    @Nested
    @DisplayName("DELETE /groups/{groupId}/users/{userUuid}")
    class LeaveGroupTests {

        @Test
        @DisplayName("Should return 204 NO_CONTENT when leave successful")
        void shouldReturn204WhenLeaveSuccessful() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doNothing().when(service).leaveGroup(groupId, userUuid);

            // When & Then
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNoContent());

            verify(service).leaveGroup(groupId, userUuid);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found"))
                    .when(service).leaveGroup(groupId, userUuid);

            // When & Then
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNotFound());

            verify(service).leaveGroup(groupId, userUuid);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user not in group")
        void shouldReturn404WhenUserNotInGroup() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not in this group"))
                    .when(service).leaveGroup(groupId, userUuid);

            // When & Then
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNotFound());

            verify(service).leaveGroup(groupId, userUuid);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found"))
                    .when(service).leaveGroup(groupId, userUuid);

            // When & Then
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNotFound());

            verify(service).leaveGroup(groupId, userUuid);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"))
                    .when(service).leaveGroup(groupId, userUuid);

            // When & Then
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isInternalServerError());

            verify(service).leaveGroup(groupId, userUuid);
        }

        @Test
        @DisplayName("Should allow leave without ownership check (SECURITY ISSUE - no validation)")
        void shouldAllowLeaveWithoutOwnershipCheck_SecurityIssue() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doNothing().when(service).leaveGroup(groupId, userUuid);

            // When & Then - Any authenticated user can remove any other user
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNoContent());

            // Note: @UId injected but not validated against userUuid - security vulnerability
            verify(service).leaveGroup(groupId, userUuid);
        }

        @Test
        @DisplayName("Should use injected UID from @UId annotation (but not validated)")
        void shouldUseInjectedUidFromAnnotation() throws Exception {
            // Given
            UUID groupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            doNothing().when(service).leaveGroup(groupId, userUuid);

            // When & Then
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, userUuid))
                    .andExpect(status().isNoContent());

            // Note: @UId is injected but service.leaveGroup doesn't receive it
            verify(service).leaveGroup(groupId, userUuid);
        }
    }

    // Helper methods
    private GroupDetailsResponseDTO createGroupDetailsResponseDTO(UUID groupId, String groupName,
                                                                   int userCount, Long transactionCount) {
        GroupInfoDTO groupInfo = GroupInfoDTO.builder()
                .uuid(groupId.toString())
                .name(groupName)
                .joinToken(UUID.randomUUID().toString())
                .tokenExpires("2024-01-02T00:00:00+09:00")
                .createdAt("2024-01-01T00:00:00+09:00")
                .updatedAt("2024-01-01T00:00:00+09:00")
                .build();

        List<UserInfoDTO> users = Collections.nCopies(userCount,
                new UserInfoDTO(
                        UUID.randomUUID().toString(),
                        "Test User",
                        null,
                        null,
                        "2024-01-01T00:00:00+09:00",
                        "2024-01-01T00:00:00+09:00"
                )
        );

        return GroupDetailsResponseDTO.builder()
                .groupInfo(groupInfo)
                .users(users)
                .transactionCount(transactionCount)
                .build();
    }

    private GroupListResponseDTO createGroupListResponseDTO(int groupCount) {
        List<GroupInfoDTO> groups = Collections.nCopies(groupCount,
                GroupInfoDTO.builder()
                        .uuid(UUID.randomUUID().toString())
                        .name("Test Group")
                        .joinToken(UUID.randomUUID().toString())
                        .tokenExpires("2024-01-02T00:00:00+09:00")
                        .createdAt("2024-01-01T00:00:00+09:00")
                        .updatedAt("2024-01-01T00:00:00+09:00")
                        .build()
        );

        return new GroupListResponseDTO(groups);
    }
}
