package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.config.BusinessRuleConfig;
import com.tateca.tatecabackend.constants.BusinessConstants;
import com.tateca.tatecabackend.repository.ObligationRepository;
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
import com.tateca.tatecabackend.exception.ErrorCode;
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

import java.time.Instant;
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
@DisplayName("GroupServiceImpl — Domain Logic")
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
    private ObligationRepository obligationRepository;

    @Mock
    private BusinessRuleConfig businessRuleConfig;

    @InjectMocks
    private GroupServiceImpl groupService;

    private static final String TEST_UID = "test-uid";
    private UUID testGroupId;
    private GroupEntity testGroup;

    @BeforeEach
    void setUp() {
        testGroupId = UUID.randomUUID();
        testGroup = TestFixtures.Groups.defaultGroup();
        testGroup.setUuid(testGroupId);
    }

    // ========================================
    // getGroupInfo
    // ========================================

    @Nested
    @DisplayName("Given グループ情報取得")
    class GetGroupInfoTests {

        @Test
        @DisplayName("Then ユーザー一覧とトランザクション件数を含むグループ情報を返す")
        void shouldReturnGroupInfoWithUsersAndTransactionCount() {
            UserEntity user1 = TestFixtures.Users.userWithoutAuthUser("User 1");
            UserEntity user2 = TestFixtures.Users.userWithoutAuthUser("User 2");
            UserGroupEntity ug1 = TestFixtures.UserGroups.create(user1, testGroup);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(user2, testGroup);

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(List.of(ug1, ug2));
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(5L);

            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            assertThat(result).isNotNull();
            assertThat(result.users()).hasSize(2);
            assertThat(result.transactionCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Then グループにユーザーがいない場合 EntityNotFoundException をスローする")
        void shouldThrowWhenGroupHasNoUsers() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> groupService.getGroupInfo(testGroupId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Then 存在しないグループの場合 EntityNotFoundException をスローする")
        void shouldThrowWhenGroupDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();
            when(userGroupRepository.findByGroupUuidWithUserDetails(nonExistentId))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> groupService.getGroupInfo(nonExistentId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ========================================
    // updateGroupName
    // ========================================

    @Nested
    @DisplayName("Given グループ名更新")
    class UpdateGroupNameTests {

        @Test
        @DisplayName("Should update group name successfully")
        void shouldUpdateGroupNameSuccessfully() {
            String newName = "Updated Group Name";
            GroupServiceImpl spy = spy(groupService);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(groupRepository.save(any(GroupEntity.class))).thenReturn(testGroup);

            GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
            doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

            GroupResponseDTO result = spy.updateGroupName(testGroupId, newName);

            assertThat(testGroup.getName()).isEqualTo(newName);
            verify(groupRepository).save(testGroup);
            assertThat(result).isEqualTo(mockResponse);
        }

        @Test
        @DisplayName("Should call getGroupInfo after update to build response")
        void shouldCallGetGroupInfoAfterUpdate() {
            GroupServiceImpl spy = spy(groupService);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(groupRepository.save(any(GroupEntity.class))).thenReturn(testGroup);

            GroupResponseDTO mockResponse = new GroupResponseDTO(null, List.of(), 0L);
            doReturn(mockResponse).when(spy).getGroupInfo(testGroupId);

            spy.updateGroupName(testGroupId, "New Name");

            verify(spy).getGroupInfo(testGroupId);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when group not found")
        void shouldThrowWhenGroupNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(groupRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.updateGroupName(nonExistentId, "New Name"))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(groupRepository, never()).save(any());
        }
    }

    // ========================================
    // getGroupList
    // ========================================

    @Nested
    @DisplayName("Given グループ一覧取得")
    class GetGroupListTests {

        @Test
        @DisplayName("Should extract user UUIDs and query user-group relationships")
        void shouldExtractUserUuidsCorrectly() {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            UserEntity user1 = TestFixtures.Users.userWithAuthUser(authUser);
            user1.setUuid(uuid1);
            UserEntity user2 = TestFixtures.Users.userWithAuthUser(authUser);
            user2.setUuid(uuid2);

            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of(user1, user2));
            when(userGroupRepository.findByUserUuidListWithGroup(anyList()))
                    .thenReturn(new ArrayList<>());

            groupService.getGroupList(TEST_UID);

            ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
            verify(userGroupRepository).findByUserUuidListWithGroup(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(uuid1, uuid2);
        }

        @Test
        @DisplayName("Should return empty list when user has no groups")
        void shouldReturnEmptyListWhenNoGroups() {
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of());

            GroupListResponseDTO result = groupService.getGroupList(TEST_UID);

            assertThat(result.groupList()).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when user does not exist")
        void shouldReturnEmptyListWhenUserDoesNotExist() {
            String nonExistentUid = "non-existent-uid";
            when(userRepository.findByAuthUserUid(nonExistentUid)).thenReturn(List.of());

            GroupListResponseDTO result = groupService.getGroupList(nonExistentUid);

            assertThat(result.groupList()).isEmpty();
        }

        @Test
        @DisplayName("Should return groups from user-group relationships")
        void shouldReturnGroupsFromUserGroupRelationships() {
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            user.setUuid(UUID.randomUUID());

            GroupEntity group1 = TestFixtures.Groups.withName("Group 1");
            GroupEntity group2 = TestFixtures.Groups.withName("Group 2");

            UserGroupEntity ug1 = TestFixtures.UserGroups.create(user, group1);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(user, group2);

            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of(user));
            when(userGroupRepository.findByUserUuidListWithGroup(anyList()))
                    .thenReturn(List.of(ug1, ug2));

            GroupListResponseDTO result = groupService.getGroupList(TEST_UID);

            assertThat(result.groupList()).hasSize(2);
        }
    }

    // ========================================
    // createGroup
    // ========================================

    @Nested
    @DisplayName("Given グループ作成")
    class CreateGroupTests {

        @Test
        @DisplayName("Should throw BusinessRuleViolationException when user at max group limit")
        void shouldThrowWhenUserAtMaxGroupLimit() {
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group", "Host", List.of("Participant"));

            List<UserEntity> existingUsers = IntStream.range(0, BusinessConstants.MAX_GROUP_PARTICIPANTS)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());

            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(existingUsers);
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("dev-unlimited-uid");

            assertThatThrownBy(() -> groupService.createGroup(TEST_UID, request))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(groupRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow unlimited UID to exceed group limit")
        void shouldAllowUnlimitedUidToExceedGroupLimit() {
            String unlimitedUid = "dev-unlimited-uid";
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group", "Host", List.of("Participant"));
            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

            List<UserEntity> existingUsers = IntStream.range(0, 10)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());

            when(userRepository.findByAuthUserUid(unlimitedUid)).thenReturn(existingUsers);
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn(unlimitedUid);
            when(authUserRepository.findById(unlimitedUid)).thenReturn(Optional.of(authUser));
            when(groupRepository.save(any(GroupEntity.class))).thenAnswer(i -> {
                GroupEntity g = i.getArgument(0);
                if (g.getTokenExpires() == null) g.setTokenExpires(Instant.now().plusSeconds(86400));
                if (g.getCreatedAt() == null) g.setCreatedAt(Instant.now());
                if (g.getUpdatedAt() == null) g.setUpdatedAt(Instant.now());
                return g;
            });
            when(userRepository.saveAll(anyList())).thenAnswer(i -> {
                List<UserEntity> users = i.getArgument(0);
                users.forEach(u -> {
                    if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
                    if (u.getUpdatedAt() == null) u.setUpdatedAt(Instant.now());
                });
                return users;
            });
            when(userGroupRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.countByGroup_Uuid(any())).thenReturn(0L);

            GroupResponseDTO result = groupService.createGroup(unlimitedUid, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when AuthUser not found")
        void shouldThrowWhenAuthUserNotFound() {
            String nonExistentUid = "non-existent-uid";
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "New Group", "Host", List.of("Participant"));

            when(userRepository.findByAuthUserUid(nonExistentUid)).thenReturn(List.of());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");
            when(groupRepository.save(any(GroupEntity.class))).thenAnswer(i -> {
                GroupEntity g = i.getArgument(0);
                if (g.getTokenExpires() == null) g.setTokenExpires(Instant.now().plusSeconds(86400));
                if (g.getCreatedAt() == null) g.setCreatedAt(Instant.now());
                if (g.getUpdatedAt() == null) g.setUpdatedAt(Instant.now());
                return g;
            });
            when(authUserRepository.findById(nonExistentUid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.createGroup(nonExistentUid, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should create group with host linked to auth user and participants without")
        void shouldCreateGroupWithCorrectUserLinking() {
            CreateGroupRequestDTO request = new CreateGroupRequestDTO(
                    "Test Group", "Host", List.of("P1", "P2"));
            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();

            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");
            when(groupRepository.save(any(GroupEntity.class))).thenAnswer(i -> {
                GroupEntity g = i.getArgument(0);
                if (g.getTokenExpires() == null) g.setTokenExpires(Instant.now().plusSeconds(86400));
                if (g.getCreatedAt() == null) g.setCreatedAt(Instant.now());
                if (g.getUpdatedAt() == null) g.setUpdatedAt(Instant.now());
                return g;
            });
            when(authUserRepository.findById(TEST_UID)).thenReturn(Optional.of(authUser));
            when(userRepository.saveAll(anyList())).thenAnswer(i -> {
                List<UserEntity> users = i.getArgument(0);
                users.forEach(u -> {
                    if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
                    if (u.getUpdatedAt() == null) u.setUpdatedAt(Instant.now());
                });
                return users;
            });
            when(userGroupRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.countByGroup_Uuid(any())).thenReturn(0L);

            GroupResponseDTO result = groupService.createGroup(TEST_UID, request);

            assertThat(result).isNotNull();
            assertThat(result.users()).hasSize(3);

            ArgumentCaptor<List<UserEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).saveAll(captor.capture());
            List<UserEntity> savedUsers = captor.getValue();
            long usersWithAuth = savedUsers.stream().filter(u -> u.getAuthUser() != null).count();
            assertThat(usersWithAuth).isEqualTo(1);
        }
    }

    // ========================================
    // joinGroupInvited
    // ========================================

    @Nested
    @DisplayName("Given グループ参加")
    class JoinGroupInvitedTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when group not found")
        void shouldThrowWhenGroupNotFound() {
            UUID nonExistentGroupId = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(UUID.randomUUID(), UUID.randomUUID());

            when(groupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.joinGroupInvited(request, nonExistentGroupId, TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BusinessRuleViolationException when user already joined group")
        void shouldThrowWhenUserAlreadyJoined() {
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            authUser.setUid(TEST_UID);
            UserEntity existingUser = TestFixtures.Users.userWithAuthUser(authUser);
            UserGroupEntity existingUG = TestFixtures.UserGroups.create(existingUser, testGroup);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(List.of(existingUG));

            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("Should throw BusinessRuleViolationException when user at max group limit")
        void shouldThrowWhenUserAtMaxGroupLimit() {
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());

            List<UserEntity> existingUsers = IntStream.range(0, BusinessConstants.MAX_GROUP_PARTICIPANTS)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>());
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(existingUsers);
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("dev-unlimited-uid");

            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("Should allow unlimited UID to join beyond limit")
        void shouldAllowUnlimitedUidToJoinBeyondLimit() {
            String unlimitedUid = "dev-unlimited-uid";
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            user.setUuid(userUuid);

            List<UserEntity> existingUsers = IntStream.range(0, 12)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>());
            when(userRepository.findByAuthUserUid(unlimitedUid)).thenReturn(existingUsers);
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn(unlimitedUid);
            when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
            when(authUserRepository.findById(unlimitedUid)).thenReturn(Optional.of(authUser));
            when(userRepository.save(any())).thenReturn(user);
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

            GroupResponseDTO result = groupService.joinGroupInvited(request, testGroupId, unlimitedUid);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw ForbiddenException when join token is invalid")
        void shouldThrowWhenJoinTokenInvalid() {
            UUID userUuid = UUID.randomUUID();
            UUID wrongToken = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, wrongToken);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>());
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");

            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user UUID not found")
        void shouldThrowWhenUserNotFound() {
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>());
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");
            when(userRepository.findById(userUuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when auth user not found")
        void shouldThrowWhenAuthUserNotFound() {
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            user.setUuid(userUuid);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>());
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(List.of());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");
            when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
            when(authUserRepository.findById(TEST_UID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should link user to auth user on successful join")
        void shouldLinkUserToAuthUserOnJoin() {
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            user.setUuid(userUuid);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>());
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(new ArrayList<>());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");
            when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
            when(authUserRepository.findById(TEST_UID)).thenReturn(Optional.of(authUser));
            when(userRepository.save(any())).thenReturn(user);
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

            groupService.joinGroupInvited(request, testGroupId, TEST_UID);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getAuthUser()).isEqualTo(authUser);
        }

        @Test
        @DisplayName("Should include existing group users in response")
        void shouldIncludeExistingUsersInResponse() {
            UUID userUuid = UUID.randomUUID();
            JoinGroupRequestDTO request = new JoinGroupRequestDTO(userUuid, testGroup.getJoinToken());

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("Joiner");
            user.setUuid(userUuid);

            UserEntity existing1 = TestFixtures.Users.userWithoutAuthUser("User 1");
            UserEntity existing2 = TestFixtures.Users.userWithoutAuthUser("User 2");
            UserGroupEntity ug1 = TestFixtures.UserGroups.create(existing1, testGroup);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(existing2, testGroup);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(List.of(ug1, ug2));
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(new ArrayList<>());
            when(businessRuleConfig.getUnlimitedGroupUid()).thenReturn("");
            when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
            when(authUserRepository.findById(TEST_UID)).thenReturn(Optional.of(authUser));
            when(userRepository.save(any())).thenReturn(user);
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(5L);

            GroupResponseDTO result = groupService.joinGroupInvited(request, testGroupId, TEST_UID);

            assertThat(result.users()).hasSize(2);
            assertThat(result.transactionCount()).isEqualTo(5L);
        }
    }

    // ========================================
    // leaveGroup
    // ========================================

    @Nested
    @DisplayName("Given グループ脱退")
    class LeaveGroupTests {

        @Test
        @DisplayName("Should nullify auth user reference on leave")
        void shouldNullifyAuthUserOnLeave() {
            UUID userUuid = UUID.randomUUID();
            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
            user.setUuid(userUuid);

            UserGroupEntity userGroup = TestFixtures.UserGroups.create(user, testGroup);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                    .thenReturn(Optional.of(userGroup));
            when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            groupService.leaveGroup(testGroupId, userUuid);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when group not found")
        void shouldThrowWhenGroupNotFound() {
            UUID nonExistentGroupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            when(groupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.leaveGroup(nonExistentGroupId, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not in group")
        void shouldThrowWhenUserNotInGroup() {
            UUID userUuid = UUID.randomUUID();

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user entity not found")
        void shouldThrowWhenUserEntityNotFound() {
            UUID userUuid = UUID.randomUUID();
            UserGroupEntity userGroup = TestFixtures.UserGroups.create(
                    TestFixtures.Users.userWithoutAuthUser("User"), testGroup);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                    .thenReturn(Optional.of(userGroup));
            when(userRepository.findById(userUuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, userUuid))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should handle user without auth user (already left)")
        void shouldHandleUserWithoutAuthUser() {
            UUID userUuid = UUID.randomUUID();
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            user.setUuid(userUuid);

            UserGroupEntity userGroup = TestFixtures.UserGroups.create(user, testGroup);

            when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));
            when(userGroupRepository.findByUserUuidAndGroupUuid(userUuid, testGroupId))
                    .thenReturn(Optional.of(userGroup));
            when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            groupService.leaveGroup(testGroupId, userUuid);

            verify(userRepository).save(user);
            assertThat(user.getAuthUser()).isNull();
        }
    }

    // ========================================
    // addMember
    // ========================================

    @Nested
    @DisplayName("Given メンバー追加")
    class AddMemberTests {

        @Test
        @DisplayName("Should add member successfully when requester is group member")
        void shouldAddMemberSuccessfully() {
            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            authUser.setUid(TEST_UID);
            UserEntity requester = TestFixtures.Users.userWithAuthUser(authUser);
            UserGroupEntity requesterUG = TestFixtures.UserGroups.create(requester, testGroup);

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>(List.of(requesterUG)));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> {
                UserEntity u = i.getArgument(0);
                if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
                if (u.getUpdatedAt() == null) u.setUpdatedAt(Instant.now());
                return u;
            });
            when(userGroupRepository.save(any(UserGroupEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            assertThat(result).isNotNull();
            assertThat(result.users()).hasSize(2);
            verify(userRepository).save(any(UserEntity.class));
            verify(userGroupRepository).save(any(UserGroupEntity.class));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when group not found (empty user groups)")
        void shouldThrowWhenGroupNotFound() {
            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> groupService.addMember(testGroupId, TEST_UID, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when requester is not group member")
        void shouldThrowWhenRequesterNotGroupMember() {
            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            UserEntity otherUser = TestFixtures.Users.userWithoutAuthUser("Other");
            UserGroupEntity otherUG = TestFixtures.UserGroups.create(otherUser, testGroup);

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(List.of(otherUG));

            assertThatThrownBy(() -> groupService.addMember(testGroupId, TEST_UID, request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should throw BusinessRuleViolationException when group at max size")
        void shouldThrowWhenGroupAtMaxSize() {
            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            authUser.setUid(TEST_UID);
            UserEntity requester = TestFixtures.Users.userWithAuthUser(authUser);

            List<UserGroupEntity> userGroups = new ArrayList<>();
            userGroups.add(TestFixtures.UserGroups.create(requester, testGroup));
            IntStream.range(0, BusinessConstants.MAX_GROUP_SIZE - 1).forEach(i -> {
                UserEntity member = TestFixtures.Users.userWithoutAuthUser("Member " + i);
                userGroups.add(TestFixtures.UserGroups.create(member, testGroup));
            });

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(userGroups);

            assertThatThrownBy(() -> groupService.addMember(testGroupId, TEST_UID, request))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("Should create new member without auth user")
        void shouldCreateMemberWithoutAuthUser() {
            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            authUser.setUid(TEST_UID);
            UserEntity requester = TestFixtures.Users.userWithAuthUser(authUser);
            UserGroupEntity requesterUG = TestFixtures.UserGroups.create(requester, testGroup);

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(new ArrayList<>(List.of(requesterUG)));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> {
                UserEntity u = i.getArgument(0);
                if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
                if (u.getUpdatedAt() == null) u.setUpdatedAt(Instant.now());
                return u;
            });
            when(userGroupRepository.save(any(UserGroupEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

            groupService.addMember(testGroupId, TEST_UID, request);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            UserEntity savedMember = captor.getValue();
            assertThat(savedMember.getName()).isEqualTo("New Member");
            assertThat(savedMember.getAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should add one under max size successfully")
        void shouldAddWhenOneUnderMaxSize() {
            AddMemberRequestDTO request = new AddMemberRequestDTO("Last Member");

            AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
            authUser.setUid(TEST_UID);
            UserEntity requester = TestFixtures.Users.userWithAuthUser(authUser);

            List<UserGroupEntity> userGroups = new ArrayList<>();
            userGroups.add(TestFixtures.UserGroups.create(requester, testGroup));
            IntStream.range(0, BusinessConstants.MAX_GROUP_SIZE - 2).forEach(i -> {
                UserEntity member = TestFixtures.Users.userWithoutAuthUser("Member " + i);
                userGroups.add(TestFixtures.UserGroups.create(member, testGroup));
            });

            when(userGroupRepository.findByGroupUuidWithUserDetails(testGroupId))
                    .thenReturn(userGroups);
            when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> {
                UserEntity u = i.getArgument(0);
                if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
                if (u.getUpdatedAt() == null) u.setUpdatedAt(Instant.now());
                return u;
            });
            when(userGroupRepository.save(any(UserGroupEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.countByGroup_Uuid(testGroupId)).thenReturn(0L);

            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            assertThat(result).isNotNull();
            assertThat(result.users()).hasSize(BusinessConstants.MAX_GROUP_SIZE);
        }
    }
}
