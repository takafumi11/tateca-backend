package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.config.BusinessRuleConfig;
import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.dto.request.AddMemberRequestDTO;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.exception.domain.BusinessRuleViolationException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.exception.domain.ForbiddenException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.GroupRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
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
class GroupServiceUnitTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BusinessRuleConfig businessRuleConfig;

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
    // updateGroupName Tests (3 tests)
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

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(GroupEntity.class))).thenReturn(testGroup);

        GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
        doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

        // When: Updating group groupName
        GroupResponseDTO result = spy.updateGroupName(testGroupId, newName);

        // Then: Should update and save
        assertThat(testGroup.getName()).isEqualTo(newName);
        verify(groupRepository).save(testGroup);
        assertThat(result).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("Should call getGroupInfo after update")
    void shouldCallGetGroupInfoAfterUpdate() {
        // Given: Group exists
        GroupServiceImpl spy = spy(groupService);

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(GroupEntity.class))).thenReturn(testGroup);

        GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
        doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

        // When: Updating group groupName
        spy.updateGroupName(testGroupId, "New Name");

        // Then: Should call getGroupInfo
        verify(spy).getGroupInfo(testGroupId);
    }
}

    // ========================================
    // getGroupList Tests (5 tests)
    // ========================================

    @Nested
    @DisplayName("getGroupList Tests")
    class GetGroupListTests {

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
        when(userGroupRepository.findByUserUuidListWithGroup(anyList()))
                .thenReturn(new ArrayList<>());

        // When: Getting group list
        groupService.getGroupList(uid);

        // Then: Should call with correct UUIDs
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(userGroupRepository).findByUserUuidListWithGroup(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(uuid1, uuid2);
    }

    }

    // ========================================
    // createGroup Tests (12 tests)
    // ========================================

    @Nested
    @DisplayName("createGroup Tests")
    class CreateGroupTests {

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
        when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("dev-unlimited-uid");

        // When & Then: Should throw conflict exception
        assertThatThrownBy(() -> groupService.createGroup(uid, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("can't join more than 10 groups");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow special UID to exceed group limit")
    void shouldAllowSpecialUidToExceedGroupLimit() {
        // Given: Special UID with 10+ groups
        String specialUid = "dev-unlimited-uid";
        CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                "New Group", "Host", List.of("Participant"));
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

        List<UserEntity> existingUsers = IntStream.range(0, 10)
                .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                .collect(Collectors.toList());

        when(userRepository.findByAuthUserUid(specialUid)).thenReturn(existingUsers);
        when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn(specialUid);
        when(authUserRepository.findById(specialUid)).thenReturn(Optional.of(authUser));
        when(groupRepository.save(any(GroupEntity.class))).thenAnswer(i -> {
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
        when(userGroupRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.countByGroup_Uuid(any())).thenReturn(0L);

        // When: Creating group
        GroupResponseDTO result = groupService.createGroup(specialUid, request);

        // Then: Should succeed
        assertThat(result).isNotNull();
    }

    }

    // ========================================
    // joinGroupInvited Tests (11 tests)
    // ========================================

    @Nested
    @DisplayName("joinGroupInvited Tests")
    class JoinGroupInvitedTests {

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

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(existingUsers);
        when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("dev-unlimited-uid");

        // When & Then: Should throw conflict exception
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("can't join more than 10 groups");
    }

    @Test
    @DisplayName("Should allow special UID to join beyond limit")
    void shouldAllowSpecialUidToJoinBeyondLimit() {
        // Given: Special UID with 10+ groups
        String specialUid = "dev-unlimited-uid";
        UUID userUuid = UUID.randomUUID();
        UUID joinToken = testGroup.getJoinToken();
        JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, joinToken);

        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
        user.setUuid(userUuid);

        List<UserEntity> existingUsers = IntStream.range(0, 12)
                .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                .collect(Collectors.toList());

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(specialUid)).thenReturn(existingUsers);
        when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn(specialUid);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(specialUid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

        // When: Joining group
        GroupResponseDTO result = groupService.joinGroupInvited(request, testGroupId, specialUid);

        // Then: Should succeed
        assertThat(result).isNotNull();
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

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(existingUG));

        // When & Then: Should detect duplicate
        assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, uid))
                .isInstanceOf(BusinessRuleViolationException.class);
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

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(new ArrayList<>());
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

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

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                .thenReturn(List.of(ug1, ug2));
        when(userRepository.findByAuthUserUid(uid)).thenReturn(new ArrayList<>());
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(authUserRepository.findById(uid)).thenReturn(Optional.of(authUser));
        when(userRepository.save(any())).thenReturn(user);
        when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(5L);

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

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenReturn(Optional.of(testUserGroup));
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // When: Leaving group
        groupService.leaveGroup(testGroupId, userUuid);

        // Then: Should nullify authUser
        verify(userRepository).save(user);
        assertThat(user.getAuthUser()).isNull();
    }

    @Test
    @DisplayName("Should nullify auth user reference")
    void shouldNullifyAuthUserReference() {
        // Given: User with authUser
        UUID userUuid = UUID.randomUUID();
        AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
        UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
        user.setUuid(userUuid);

        when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
        when(userGroupRepository.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                .thenReturn(Optional.of(testUserGroup));
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // When: Leaving group
        groupService.leaveGroup(testGroupId, userUuid);

        // Then: Should set authUser to null
        assertThat(user.getAuthUser()).isNull();
    }

    }

    // ========================================
    // addMember Tests (1 test)
    // ========================================

    @Nested
    @DisplayName("addMember Tests")
    class AddMemberTests {

        @Test
        @DisplayName("Should count current members using userGroupEntityList size")
        void shouldCountCurrentMembersUsingUserGroupEntityListSize() {
            // Given: Group with 10 members (at maximum)
            String uid = "test-uid";
            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            authUser.setUid(uid);
            UserEntity requester = TestFixtures.Users.userWithAuthUser(authUser);

            // Create exactly 10 user-group relationships
            List<UserGroupEntity> userGroups = new ArrayList<>();
            userGroups.add(TestFixtures.UserGroups.create(requester, testGroup));
            IntStream.range(0, 9).forEach(i -> {
                UserEntity member = TestFixtures.Users.userWithoutAuthUser("Member " + i);
                userGroups.add(TestFixtures.UserGroups.create(member, testGroup));
            });

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(userGroups);

            // When & Then: Should throw exception because size == 10 (maximum)
            assertThatThrownBy(() -> groupService.addMember(testGroupId, uid, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Group has reached maximum size of 10 members");
        }
    }
}
