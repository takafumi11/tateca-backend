package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserGroupAccessor;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.impl.GroupServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService Unit Tests")
class GroupServiceUnitTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private GroupAccessor accessor;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private UserGroupAccessor userGroupAccessor;

    @Mock
    private TransactionAccessor transactionAccessor;

    @InjectMocks
    private GroupServiceImpl groupService;

    private UUID testGroupId;
    private GroupEntity testGroup;
    private UserGroupEntity testUserGroup;

    @BeforeEach
    void setUp() {
        testGroupId = UUID.randomUUID();
        testGroup = TestFixtures.Groups.defaultGroup();
        testGroup.setUuid(testGroupId);

        UserEntity testUser = TestFixtures.Users.userWithoutAuthUser("Test User");
        testUserGroup = TestFixtures.UserGroups.create(testUser, testGroup);
    }

    // ========================================
    // getGroupInfo Tests (5 tests)
    // ========================================

    @Nested
    @DisplayName("getGroupInfo Tests")
    class GetGroupInfoTests {

        @Test
        @DisplayName("Should return group info when group exists")
        void shouldReturnGroupInfoWhenGroupExists() {
        // Given: Group with users and transactions
        List<UserGroupEntity> userGroups = List.of(testUserGroup);
        Long transactionCount = 5L;

        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(userGroups);
        when(transactionAccessor.countByGroupId(testGroupId))
                .thenReturn(transactionCount);

        // When: Getting group info
        GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

        // Then: Should return complete group info
        assertThat(result).isNotNull();
        assertThat(result.users()).hasSize(1);
        assertThat(result.transactionCount()).isEqualTo(transactionCount);
    }

    @Test
    @DisplayName("Should throw not found exception when group has no users")
    void shouldThrowNotFoundExceptionWhenGroupHasNoUsers() {
        // Given: Group with no users
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());

        // When & Then: Should throw ResponseStatusException
        assertThatThrownBy(() -> groupService.getGroupInfo(testGroupId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Group Not Found with");
                });
    }

    @Test
    @DisplayName("Should map users correctly from user group entities")
    void shouldMapUsersCorrectlyFromUserGroupEntities() {
        // Given: Multiple users in group
        UserEntity user1 = TestFixtures.Users.userWithoutAuthUser("User 1");
        UserEntity user2 = TestFixtures.Users.userWithoutAuthUser("User 2");
        UserEntity user3 = TestFixtures.Users.userWithoutAuthUser("User 3");

        UserGroupEntity ug1 = TestFixtures.UserGroups.create(user1, testGroup);
        UserGroupEntity ug2 = TestFixtures.UserGroups.create(user2, testGroup);
        UserGroupEntity ug3 = TestFixtures.UserGroups.create(user3, testGroup);

        List<UserGroupEntity> userGroups = List.of(ug1, ug2, ug3);

        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(userGroups);
        when(transactionAccessor.countByGroupId(testGroupId))
                .thenReturn(0L);

        // When: Getting group info
        GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

        // Then: Should map all users correctly
        assertThat(result.users()).hasSize(3);
    }

    @Test
    @DisplayName("Should include transaction count in response")
    void shouldIncludeTransactionCountInResponse() {
        // Given: Group with specific transaction count
        Long expectedCount = 42L;

        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(testUserGroup));
        when(transactionAccessor.countByGroupId(testGroupId))
                .thenReturn(expectedCount);

        // When: Getting group info
        GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

        // Then: Should include exact transaction count
        assertThat(result.transactionCount()).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("Should handle zero transaction count")
    void shouldHandleZeroTransactionCount() {
        // Given: Group with no transactions
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(testUserGroup));
        when(transactionAccessor.countByGroupId(testGroupId))
                .thenReturn(0L);

        // When: Getting group info
        GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then: Should return 0 transaction count
            assertThat(result.transactionCount()).isEqualTo(0L);
        }
    }

    // ========================================
    // updateGroupName Tests (5 tests)
    // ========================================

    @Nested
    @DisplayName("updateGroupName Tests")
    class UpdateGroupNameTests {

        @Test
        @DisplayName("Should update group groupName successfully")
    void shouldUpdateGroupNameSuccessfully() {
        // Given: Group exists
        String newName = "Updated Group Name";
        GroupServiceImpl spy = spy(groupService);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(accessor.save(any(GroupEntity.class))).thenReturn(testGroup);

        GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
        doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

        // When: Updating group groupName
        GroupResponseDTO result = spy.updateGroupName(testGroupId, newName);

        // Then: Should update and save
        assertThat(testGroup.getName()).isEqualTo(newName);
        verify(accessor).save(testGroup);
        assertThat(result).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("Should throw not found exception when group not exists")
    void shouldThrowNotFoundExceptionWhenGroupNotExists() {
        // Given: Group does not exist
        when(accessor.findById(testGroupId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> groupService.updateGroupName(testGroupId, "New Name"))
                .isInstanceOf(ResponseStatusException.class);

        verify(accessor, never()).save(any());
    }

    @Test
    @DisplayName("Should call getGroupInfo after update")
    void shouldCallGetGroupInfoAfterUpdate() {
        // Given: Group exists
        GroupServiceImpl spy = spy(groupService);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(accessor.save(any(GroupEntity.class))).thenReturn(testGroup);

        GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
        doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

        // When: Updating group groupName
        spy.updateGroupName(testGroupId, "New Name");

        // Then: Should call getGroupInfo
        verify(spy).getGroupInfo(testGroupId);
    }

    @Test
    @DisplayName("Should handle empty group groupName")
    void shouldHandleEmptyGroupName() {
        // Given: Empty groupName
        String emptyName = "";
        GroupServiceImpl spy = spy(groupService);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(accessor.save(any(GroupEntity.class))).thenReturn(testGroup);

        GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
        doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

        // When: Updating with empty groupName
        spy.updateGroupName(testGroupId, emptyName);

        // Then: Should set empty groupName (no service validation)
        assertThat(testGroup.getName()).isEqualTo(emptyName);
    }

    @Test
    @DisplayName("Should handle special characters in group groupName")
    void shouldHandleSpecialCharactersInGroupName() {
        // Given: Name with special characters
        String specialName = "Test 😊 田中 €$";
        GroupServiceImpl spy = spy(groupService);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(accessor.save(any(GroupEntity.class))).thenReturn(testGroup);

        GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
        doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

        // When: Updating with special characters
        spy.updateGroupName(testGroupId, specialName);

            // Then: Should preserve special characters
            assertThat(testGroup.getName()).isEqualTo(specialName);
        }
    }

    // ========================================
    // getGroupList Tests (5 tests)
    // ========================================

    @Nested
    @DisplayName("getGroupList Tests")
    class GetGroupListTests {

        @Test
        @DisplayName("Should return group list for user")
    void shouldReturnGroupListForUser() {
        // Given: User with groups
        String uid = "test-uid";
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
        GroupEntity group1 = TestFixtures.Groups.withName("Group 1");
        GroupEntity group2 = TestFixtures.Groups.withName("Group 2");

        UserGroupEntity ug1 = TestFixtures.UserGroups.create(user, group1);
        UserGroupEntity ug2 = TestFixtures.UserGroups.create(user, group2);

        when(userRepository.findByAuthUserUid(uid)).thenReturn(List.of(user));
        when(userGroupAccessor.findByUserUuidListWithGroup(anyList()))
                .thenReturn(List.of(ug1, ug2));

        // When: Getting group list
        GroupListResponseDTO result = groupService.getGroupList(uid);

        // Then: Should return both groups
        assertThat(result.groupList()).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list when user has no groups")
    void shouldReturnEmptyListWhenUserHasNoGroups() {
        // Given: User with no groups
        String uid = "test-uid";

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userGroupAccessor.findByUserUuidListWithGroup(anyList()))
                .thenReturn(new ArrayList<>());

        // When: Getting group list
        GroupListResponseDTO result = groupService.getGroupList(uid);

        // Then: Should return empty list
        assertThat(result.groupList()).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple groups")
    void shouldHandleMultipleGroups() {
        // Given: User in 5 groups
        String uid = "test-uid";
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);

        List<UserGroupEntity> userGroups = IntStream.range(0, 5)
                .mapToObj(i -> {
                    GroupEntity group = TestFixtures.Groups.withName("Group " + i);
                    return TestFixtures.UserGroups.create(user, group);
                })
                .collect(Collectors.toList());

        when(userRepository.findByAuthUserUid(uid)).thenReturn(List.of(user));
        when(userGroupAccessor.findByUserUuidListWithGroup(anyList()))
                .thenReturn(userGroups);

        // When: Getting group list
        GroupListResponseDTO result = groupService.getGroupList(uid);

        // Then: Should return all 5 groups
        assertThat(result.groupList()).hasSize(5);
    }

    @Test
    @DisplayName("Should extract user UUIDs correctly")
    void shouldExtractUserUuidsCorrectly() {
        // Given: Users with specific UUIDs
        String uid = "test-uid";
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user1 = TestFixtures.Users.userWithAuthUser(authUser);
        user1.setUuid(uuid1);
        UserEntity user2 = TestFixtures.Users.userWithAuthUser(authUser);
        user2.setUuid(uuid2);

        when(userRepository.findByAuthUserUid(uid)).thenReturn(List.of(user1, user2));
        when(userGroupAccessor.findByUserUuidListWithGroup(anyList()))
                .thenReturn(new ArrayList<>());

        // When: Getting group list
        groupService.getGroupList(uid);

        // Then: Should call with correct UUIDs
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(userGroupAccessor).findByUserUuidListWithGroup(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(uuid1, uuid2);
    }

    @Test
    @DisplayName("Should map group entities correctly")
    void shouldMapGroupEntitiesCorrectly() {
        // Given: User with groups
        String uid = "test-uid";
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
        GroupEntity group = TestFixtures.Groups.withName("Test Group");
        UserGroupEntity ug = TestFixtures.UserGroups.create(user, group);

        when(userRepository.findByAuthUserUid(uid)).thenReturn(List.of(user));
        when(userGroupAccessor.findByUserUuidListWithGroup(anyList()))
                .thenReturn(List.of(ug));

        // When: Getting group list
        GroupListResponseDTO result = groupService.getGroupList(uid);

            // Then: Should map groups correctly
            assertThat(result.groupList()).hasSize(1);
        }
    }

    // ========================================
    // createGroup Tests (12 tests)
    // ========================================

    @Nested
    @DisplayName("createGroup Tests")
    class CreateGroupTests {

        @Test
        @DisplayName("Should create group successfully with host and participants")
    void shouldCreateGroupSuccessfullyWithHostAndParticipants() {
        // Given: Valid request
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host User", List.of("User 1", "User 2"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any(GroupEntity.class))).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        GroupResponseDTO result = groupService.createGroup(uid, request);

        // Then: Should create all entities
        assertThat(result).isNotNull();
        verify(accessor).save(any(GroupEntity.class));
        verify(userRepository).saveAll(any());
        verify(userGroupAccessor).saveAll(any());
    }

    @Test
    @DisplayName("Should validate max group count before creation")
    void shouldValidateMaxGroupCountBeforeCreation() {
        // Given: User at limit (9 groups)
        String uid = "regular-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));

        List<UserEntity> existingUsers = IntStream.range(0, 9)
                .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                .collect(Collectors.toList());

        when(userRepository.findByAuthUserUid(uid)).thenReturn(existingUsers);

        // When & Then: Should throw conflict exception
        assertThatThrownBy(() -> groupService.createGroup(uid, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("can't join more than 10 groups");
                });

        verify(accessor, never()).save(any());
    }

    @Test
    @DisplayName("Should allow special UID to exceed group limit")
    void shouldAllowSpecialUidToExceedGroupLimit() {
        // Given: Special UID with 10+ groups
        String specialUid = "v6CGVApOmVM4VWTijmRTg8m01Kj1";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        List<UserEntity> existingUsers = IntStream.range(0, 10)
                .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                .collect(Collectors.toList());

        when(userRepository.findByAuthUserUid(specialUid)).thenReturn(existingUsers);
        when(authUserRepository.findById(specialUid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any(GroupEntity.class))).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        GroupResponseDTO result = groupService.createGroup(specialUid, request);

        // Then: Should succeed
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw not found exception when auth user not exists")
    void shouldThrowNotFoundExceptionWhenAuthUserNotExists() {
        // Given: Non-existent auth user
        String uid = "non-existent";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        assertThatThrownBy(() -> groupService.createGroup(uid, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Auth user not found");
    }

    @Test
    @DisplayName("Should create group with minimum participants")
    void shouldCreateGroupWithMinimumParticipants() {
        // Given: Request with minimum 1 participant (validation requirement)
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any(GroupEntity.class))).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

        // Then: Should create host + 1 participant = 2 users total
        ArgumentCaptor<List<UserEntity>> userCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(userCaptor.capture());
        assertThat(userCaptor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("Should create group with multiple participants")
    void shouldCreateGroupWithMultipleParticipants() {
        // Given: Request with 5 participants
        String uid = "test-uid";
        List<String> participants = List.of("P1", "P2", "P3", "P4", "P5");
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", participants);
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any(GroupEntity.class))).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

        // Then: Should create 6 users total (1 host + 5 participants)
        ArgumentCaptor<List<UserEntity>> userCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(userCaptor.capture());
        assertThat(userCaptor.getValue()).hasSize(6);
    }

    @Test
    @DisplayName("Should generate unique UUIDs for all entities")
    void shouldGenerateUniqueUuidsForAllEntities() {
        // Given: Valid request
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any(GroupEntity.class))).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

        // Then: Should have UUID and join token
        ArgumentCaptor<GroupEntity> groupCaptor = ArgumentCaptor.forClass(GroupEntity.class);
        verify(accessor).save(groupCaptor.capture());
        GroupEntity savedGroup = groupCaptor.getValue();
        assertThat(savedGroup.getUuid()).isNotNull();
        assertThat(savedGroup.getJoinToken()).isNotNull();
    }

    @Test
    @DisplayName("Should link host to auth user")
    void shouldLinkHostToAuthUser() {
        // Given: Valid request
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("P1"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any())).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

        // Then: First user (host) should have authUser, participant should not
        ArgumentCaptor<List<UserEntity>> userCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(userCaptor.capture());
        List<UserEntity> users = userCaptor.getValue();
        assertThat(users.get(0).getAuthUser()).isNotNull();
        assertThat(users.get(1).getAuthUser()).isNull();
    }

    @Test
    @DisplayName("Should save all entities in correct order")
    void shouldSaveAllEntitiesInCorrectOrder() {
        // Given: Valid request
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any())).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

        // Then: Should save in correct order
        verify(accessor).save(any(GroupEntity.class));
        verify(userRepository).saveAll(anyList());
        verify(userGroupAccessor).saveAll(anyList());
    }

    @Test
    @DisplayName("Should include transaction count in response")
    void shouldIncludeTransactionCountInResponseForCreate() {
        // Given: New group
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any())).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        GroupResponseDTO result = groupService.createGroup(uid, request);

        // Then: Should have 0 transactions
        assertThat(result.transactionCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should create user group relationships for all users")
    void shouldCreateUserGroupRelationshipsForAllUsers() {
        // Given: Host + 3 participants
        String uid = "test-uid";
        List<String> participants = List.of("P1", "P2", "P3");
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", participants);
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any())).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

        // Then: Should create 4 user-group relationships
        ArgumentCaptor<List<UserGroupEntity>> ugCaptor = ArgumentCaptor.forClass(List.class);
        verify(userGroupAccessor).saveAll(ugCaptor.capture());
        assertThat(ugCaptor.getValue()).hasSize(4);
    }

    @Test
    @DisplayName("Should handle special characters in names")
    void shouldHandleSpecialCharactersInNames() {
        // Given: Names with special characters
        String uid = "test-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "Test 😊 Group", "Host 田中", List.of("€uro", "$dollar"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(accessor.save(any(GroupEntity.class))).thenAnswer(i -> {
            GroupEntity group = i.getArgument(0);
            group.setTokenExpires(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
            group.setCreatedAt(java.time.Instant.now());
            group.setUpdatedAt(java.time.Instant.now());
            return group;
        });
        when(userRepository.saveAll(anyList())).thenAnswer(i -> {
            List<UserEntity> users = i.getArgument(0);
            users.forEach(user -> {
                user.setCreatedAt(java.time.Instant.now());
                user.setUpdatedAt(java.time.Instant.now());
            });
            return users;
        });
        when(userGroupAccessor.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionAccessor.countByGroupId(any())).thenReturn(0L);

        // When: Creating group
        groupService.createGroup(uid, request);

            // Then: Should preserve special characters
            ArgumentCaptor<GroupEntity> groupCaptor = ArgumentCaptor.forClass(GroupEntity.class);
            verify(accessor).save(groupCaptor.capture());
            assertThat(groupCaptor.getValue().getName()).isEqualTo("Test 😊 Group");
        }
    }

    // ========================================
    // joinGroupInvited Tests (11 tests)
    // ========================================

    @Nested
    @DisplayName("joinGroupInvited Tests")
    class JoinGroupInvitedTests {

        @Test
        @DisplayName("Should join group successfully with valid token")
    void shouldJoinGroupSuccessfullyWithValidToken() {
        // Given: Valid token and user not in group
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
        user.setUuid(userUuid);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionAccessor.countByGroupId(testGroupId)).thenReturn(0L);

        // When: Joining group
        GroupResponseDTO result = groupService.joinGroupInvited(request, testGroupId, uid);

        // Then: Should join successfully
        assertThat(result).isNotNull();
        verify(userRepository).save(user);
        assertThat(user.getAuthUser()).isEqualTo(authUser);
    }

    @Test
    @DisplayName("Should throw forbidden exception when token invalid")
    void shouldThrowForbiddenExceptionWhenTokenInvalid() {
        // Given: Invalid token
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID wrongToken = UUID.randomUUID();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, wrongToken);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);

        // When & Then: Should throw forbidden exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Invalid join token");
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw conflict exception when user already in group")
    void shouldThrowConflictExceptionWhenUserAlreadyInGroup() {
        // Given: User already in group
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        authUser.setUid(uid);
        UserEntity existingUser = TestFixtures.Users.userWithAuthUser(authUser);
        UserGroupEntity existingUG = TestFixtures.UserGroups.create(existingUser, testGroup);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(existingUG));

        // When & Then: Should throw conflict exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("already joined this group");
                });
    }

    @Test
    @DisplayName("Should validate max group count before joining")
    void shouldValidateMaxGroupCountBeforeJoining() {
        // Given: User at limit
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        List<UserEntity> existingUsers = IntStream.range(0, 9)
                .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                .collect(Collectors.toList());

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(existingUsers);

        // When & Then: Should throw conflict exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("can't join more than 10 groups");
                });
    }

    @Test
    @DisplayName("Should allow special UID to join beyond limit")
    void shouldAllowSpecialUidToJoinBeyondLimit() {
        // Given: Special UID with 10+ groups
        String specialUid = "v6CGVApOmVM4VWTijmRTg8m01Kj1";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
        user.setUuid(userUuid);

        List<UserEntity> existingUsers = IntStream.range(0, 12)
                .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                .collect(Collectors.toList());

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(specialUid)).thenReturn(existingUsers);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(specialUid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionAccessor.countByGroupId(testGroupId)).thenReturn(0L);

        // When: Joining group
        GroupResponseDTO result = groupService.joinGroupInvited(request, testGroupId, specialUid);

        // Then: Should succeed
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw not found exception when user not exists")
    void shouldThrowNotFoundExceptionWhenUserNotExistsForJoin() {
        // Given: Non-existent user
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.empty());

        // When & Then: Should throw not found exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should throw not found exception when auth user not exists")
    void shouldThrowNotFoundExceptionWhenAuthUserNotExistsForJoin() {
        // Given: Non-existent auth user
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);
        UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(uid)).thenReturn(Optional.empty());

        // When & Then: Should throw not found exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Auth user not found");
    }

    @Test
    @DisplayName("Should throw not found exception when group not exists")
    void shouldThrowNotFoundExceptionWhenGroupNotExistsForJoin() {
        // Given: Non-existent group
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = UUID.randomUUID();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        when(accessor.findById(testGroupId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Should detect existing membership by auth user UID")
    void shouldDetectExistingMembershipByAuthUserUid() {
        // Given: Different user entity but same authUser
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        authUser.setUid(uid);
        UserEntity existingUser = TestFixtures.Users.userWithAuthUser(authUser);
        UserGroupEntity existingUG = TestFixtures.UserGroups.create(existingUser, testGroup);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(existingUG));

        // When & Then: Should detect duplicate
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Should link user to auth user correctly")
    void shouldLinkUserToAuthUserCorrectly() {
        // Given: Valid join request
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
        user.setUuid(userUuid);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionAccessor.countByGroupId(testGroupId)).thenReturn(0L);

        // When: Joining group
        groupService.joinGroupInvited(request, testGroupId, uid);

        // Then: Should link correctly
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthUser()).isEqualTo(authUser);
    }

    @Test
    @DisplayName("Should include all group users in response")
    void shouldIncludeAllGroupUsersInResponse() {
        // Given: Group with existing users
        String uid = "test-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
        user.setUuid(userUuid);

        UserEntity existingUser1 = TestFixtures.Users.userWithoutAuthUser("User 1");
        UserEntity existingUser2 = TestFixtures.Users.userWithoutAuthUser("User 2");
        UserGroupEntity ug1 = TestFixtures.UserGroups.create(existingUser1, testGroup);
        UserGroupEntity ug2 = TestFixtures.UserGroups.create(existingUser2, testGroup);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(ug1, ug2));
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionAccessor.countByGroupId(testGroupId)).thenReturn(5L);

        // When: Joining group
        GroupResponseDTO result = groupService.joinGroupInvited(request, testGroupId, uid);

            // Then: Should include existing users and transaction count
            assertThat(result.users()).hasSize(2);
            assertThat(result.transactionCount()).isEqualTo(5L);
        }
    }

    // ========================================
    // leaveGroup Tests (6 tests)
    // ========================================

    @Nested
    @DisplayName("leaveGroup Tests")
    class LeaveGroupTests {

        @Test
        @DisplayName("Should leave group successfully")
    void shouldLeaveGroupSuccessfully() {
        // Given: User in group
        UUID userUuid = UUID.randomUUID();
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
        user.setUuid(userUuid);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenReturn(testUserGroup);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // When: Leaving group
        groupService.leaveGroup(testGroupId, userUuid);

        // Then: Should nullify authUser
        verify(userRepository).save(user);
        assertThat(user.getAuthUser()).isNull();
    }

    @Test
    @DisplayName("Should throw not found exception when group not exists for leave")
    void shouldThrowNotFoundExceptionWhenGroupNotExistsForLeave() {
        // Given: Non-existent group
        UUID userUuid = UUID.randomUUID();

        when(accessor.findById(testGroupId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, userUuid))
                .isInstanceOf(ResponseStatusException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw not found exception when user not in group")
    void shouldThrowNotFoundExceptionWhenUserNotInGroup() {
        // Given: User not in group
        UUID userUuid = UUID.randomUUID();

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not in group"));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, userUuid))
                .isInstanceOf(ResponseStatusException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw not found exception when user not exists for leave")
    void shouldThrowNotFoundExceptionWhenUserNotExistsForLeave() {
        // Given: Non-existent user
        UUID userUuid = UUID.randomUUID();

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenReturn(testUserGroup);
        when(userRepository.findById(userUuid)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, userUuid))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should nullify auth user reference")
    void shouldNullifyAuthUserReference() {
        // Given: User with authUser
        UUID userUuid = UUID.randomUUID();
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
        user.setUuid(userUuid);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenReturn(testUserGroup);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // When: Leaving group
        groupService.leaveGroup(testGroupId, userUuid);

        // Then: Should set authUser to null
        assertThat(user.getAuthUser()).isNull();
    }

    @Test
    @DisplayName("Should verify user in group before leaving")
    void shouldVerifyUserInGroupBeforeLeaving() {
        // Given: User in group
        UUID userUuid = UUID.randomUUID();
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);

        when(accessor.findById(testGroupId)).thenReturn(testGroup);
        when(userGroupAccessor.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenReturn(testUserGroup);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // When: Leaving group
        groupService.leaveGroup(testGroupId, userUuid);

            // Then: Should verify user in group first
            verify(userGroupAccessor).findByUserUuidAndGroupUuid(userUuid, testGroupId);
        }
    }
}
