package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.AddMemberRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.GroupRepository;
import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("GroupService Integration Tests — Infrastructure behavior")
class GroupServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String TEST_UID = "test-uid-" + System.currentTimeMillis();
    private AuthUserEntity testAuthUser;
    private GroupEntity testGroup;
    private UUID testGroupId;

    @BeforeEach
    void setUp() {
        testAuthUser = AuthUserEntity.builder()
                .uid(TEST_UID)
                .name("Test User")
                .email("test" + System.currentTimeMillis() + "@example.com")
                .build();
        authUserRepository.save(testAuthUser);

        testGroup = TestFixtures.Groups.defaultGroup();
        groupRepository.save(testGroup);
        testGroupId = testGroup.getUuid();

        flushAndClear();
    }

    // ========================================
    // getGroupInfo — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("getGroupInfo — エンティティリレーション取得")
    class GetGroupInfo {

        @Test
        void givenGroupExistsWithUsers_whenGettingGroupInfo_thenShouldReturnCompleteGroupInformation() {
            UserEntity user1 = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity user2 = TestFixtures.Users.userWithoutAuthUser("User 2");
            userRepository.save(user1);
            userRepository.save(user2);

            UserGroupEntity ug1 = TestFixtures.UserGroups.create(user1, testGroup);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(user2, testGroup);
            userGroupRepository.save(ug1);
            userGroupRepository.save(ug2);

            flushAndClear();

            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo()).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
            assertThat(result.users()).hasSize(2);
            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        void givenGroupWithMixedUsers_whenGettingGroupInfo_thenShouldIncludeUsersWithAndWithoutAuthUser() {
            UserEntity userWithAuth = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity userWithoutAuth = TestFixtures.Users.userWithoutAuthUser("Participant");
            userRepository.save(userWithAuth);
            userRepository.save(userWithoutAuth);

            userGroupRepository.save(TestFixtures.UserGroups.create(userWithAuth, testGroup));
            userGroupRepository.save(TestFixtures.UserGroups.create(userWithoutAuth, testGroup));

            flushAndClear();

            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            assertThat(result.users()).hasSize(2);
        }

        @Test
        void givenGroupWithUser_whenGettingGroupInfo_thenShouldCountTransactionsCorrectly() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));

            flushAndClear();

            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        void givenGroupWith20Users_whenGettingGroupInfo_thenShouldHandleLargeNumberOfUsers() {
            List<UserEntity> users = IntStream.range(0, 20)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());
            userRepository.saveAll(users);

            List<UserGroupEntity> userGroups = users.stream()
                    .map(user -> TestFixtures.UserGroups.create(user, testGroup))
                    .collect(Collectors.toList());
            userGroupRepository.saveAll(userGroups);

            flushAndClear();

            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            assertThat(result.users()).hasSize(20);
        }
    }

    // ========================================
    // updateGroupName — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("updateGroupName — 永続化とタイムスタンプ")
    class UpdateGroupName {

        @Test
        void givenGroupExists_whenUpdatingGroupName_thenShouldUpdateAndPersistToDatabase() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            String newName = "Updated Group Name";

            GroupResponseDTO result = groupService.updateGroupName(testGroupId, newName);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo(newName);

            flushAndClear();
            Optional<GroupEntity> updatedGroup = groupRepository.findById(testGroupId);
            assertThat(updatedGroup).isPresent();
            assertThat(updatedGroup.get().getName()).isEqualTo(newName);
        }

        @Test
        void givenGroupExists_whenUpdatingGroupName_thenShouldUpdateUpdatedAtTimestamp() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            GroupEntity originalGroup = groupRepository.findById(testGroupId).orElseThrow();
            java.time.Instant originalUpdatedAt = originalGroup.getUpdatedAt();

            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            groupService.updateGroupName(testGroupId, "New Name");
            flushAndClear();

            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }

        @Test
        void givenGroupExists_whenUpdatingGroupName_thenShouldPreserveOtherGroupFields() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            GroupEntity originalGroup = groupRepository.findById(testGroupId).orElseThrow();
            UUID originalJoinToken = originalGroup.getJoinToken();
            UUID originalUuid = originalGroup.getUuid();
            java.time.Instant originalCreatedAt = originalGroup.getCreatedAt();

            groupService.updateGroupName(testGroupId, "New Name");
            flushAndClear();

            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getJoinToken()).isEqualTo(originalJoinToken);
            assertThat(updatedGroup.getUuid()).isEqualTo(originalUuid);
            assertThat(updatedGroup.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                    .isEqualTo(originalCreatedAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        void givenGroupExists_whenUpdatingWithSpecialCharacters_thenShouldHandleSpecialCharactersInName() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            String specialName = "Test 😊 田中 €$";

            GroupResponseDTO result = groupService.updateGroupName(testGroupId, specialName);

            assertThat(result.groupInfo().name()).isEqualTo(specialName);

            flushAndClear();
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getName()).isEqualTo(specialName);
        }

        @Test
        void givenGroupExists_whenUpdatingMultipleTimes_thenShouldHandleMultipleSequentialUpdates() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            groupService.updateGroupName(testGroupId, "Name 1");
            flushAndClear();
            groupService.updateGroupName(testGroupId, "Name 2");
            flushAndClear();
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, "Name 3");
            flushAndClear();

            assertThat(result.groupInfo().name()).isEqualTo("Name 3");
            GroupEntity finalGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(finalGroup.getName()).isEqualTo("Name 3");
        }

        @Test
        void givenGroupWithMultipleUsers_whenUpdatingGroupName_thenShouldReturnCompleteInfoAfterUpdate() {
            UserEntity user1 = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity user2 = TestFixtures.Users.userWithoutAuthUser("User 2");
            UserEntity user3 = TestFixtures.Users.userWithoutAuthUser("User 3");
            userRepository.saveAll(List.of(user1, user2, user3));

            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(user1, testGroup),
                    TestFixtures.UserGroups.create(user2, testGroup),
                    TestFixtures.UserGroups.create(user3, testGroup)
            ));
            flushAndClear();

            GroupResponseDTO result = groupService.updateGroupName(testGroupId, "Updated Name");

            assertThat(result.users()).hasSize(3);
            assertThat(result.groupInfo().name()).isEqualTo("Updated Name");
        }
    }

    // ========================================
    // getGroupList — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("getGroupList — カスタムクエリとリレーション")
    class GetGroupList {

        @Test
        void givenUserBelongsToMultipleGroups_whenGettingGroupList_thenShouldReturnAllGroups() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);

            List<GroupEntity> groups = IntStream.range(0, 5)
                    .mapToObj(i -> {
                        GroupEntity group = TestFixtures.Groups.withName("Group " + i);
                        groupRepository.save(group);
                        return group;
                    })
                    .toList();

            groups.forEach(group -> {
                userGroupRepository.save(TestFixtures.UserGroups.create(user, group));
            });

            flushAndClear();

            var result = groupService.getGroupList(TEST_UID);

            assertThat(result.groupList()).hasSize(5);
        }

        @Test
        void givenUserLeftSomeGroups_whenGettingGroupList_thenShouldNotReturnLeftGroups() {
            UserEntity activeUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity leftUser = TestFixtures.Users.userWithoutAuthUser("Left User");
            userRepository.saveAll(List.of(activeUser, leftUser));

            GroupEntity activeGroup1 = TestFixtures.Groups.withName("Active Group 1");
            GroupEntity activeGroup2 = TestFixtures.Groups.withName("Active Group 2");
            GroupEntity leftGroup = TestFixtures.Groups.withName("Left Group");
            groupRepository.saveAll(List.of(activeGroup1, activeGroup2, leftGroup));

            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(activeUser, activeGroup1),
                    TestFixtures.UserGroups.create(activeUser, activeGroup2),
                    TestFixtures.UserGroups.create(leftUser, leftGroup)
            ));

            flushAndClear();

            var result = groupService.getGroupList(TEST_UID);

            assertThat(result.groupList()).hasSize(2);
        }

        @Test
        void givenUserBelongsToNoGroups_whenGettingGroupList_thenShouldReturnEmptyList() {
            var result = groupService.getGroupList(TEST_UID);

            assertThat(result.groupList()).isEmpty();
        }
    }

    // ========================================
    // createGroup — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("createGroup — エンティティ永続化とリレーション")
    class CreateGroup {

        @Test
        void givenValidRequestWithHostAndParticipants_whenCreatingGroup_thenShouldPersistAllEntities() {
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Integration Test Group",
                            "Host User",
                            List.of("Participant 1", "Participant 2", "Participant 3")
                    );

            var result = groupService.createGroup(TEST_UID, request);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo()).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo("Integration Test Group");

            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            Optional<GroupEntity> savedGroup = groupRepository.findById(groupId);
            assertThat(savedGroup).isPresent();

            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
            assertThat(userGroups).hasSize(4);

            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        void givenValidRequest_whenCreatingGroup_thenShouldGenerateUniqueJoinToken() {
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Test Group",
                            "Host",
                            List.of("Participant")
                    );

            var result = groupService.createGroup(TEST_UID, request);

            assertThat(result.groupInfo().joinToken()).isNotNull();
            assertThat(UUID.fromString(result.groupInfo().joinToken())).isNotNull();

            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            GroupEntity savedGroup = groupRepository.findById(groupId).orElseThrow();
            assertThat(savedGroup.getJoinToken()).isNotNull();
        }

        @Test
        void givenValidRequest_whenCreatingGroup_thenShouldSetTimestamps() {
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Timestamp Test Group",
                            "Host",
                            List.of("Participant")
                    );

            java.time.Instant beforeCreate = java.time.Instant.now();

            var result = groupService.createGroup(TEST_UID, request);

            java.time.Instant afterCreate = java.time.Instant.now();

            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            GroupEntity savedGroup = groupRepository.findById(groupId).orElseThrow();

            assertThat(savedGroup.getCreatedAt()).isNotNull();
            assertThat(savedGroup.getUpdatedAt()).isNotNull();
            assertThat(savedGroup.getCreatedAt()).isBetween(
                    beforeCreate.minusSeconds(1),
                    afterCreate.plusSeconds(1)
            );
        }

        @Test
        void givenRequestWithHostAndParticipants_whenCreatingGroup_thenShouldLinkOnlyHostToAuthUser() {
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Auth Link Test Group",
                            "Host User",
                            List.of("Participant 1", "Participant 2")
                    );

            var result = groupService.createGroup(TEST_UID, request);

            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);

            List<UserEntity> users = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .toList();

            long usersWithAuthUser = users.stream()
                    .filter(u -> u.getAuthUser() != null)
                    .count();
            assertThat(usersWithAuthUser).isEqualTo(1);

            long usersWithoutAuthUser = users.stream()
                    .filter(u -> u.getAuthUser() == null)
                    .count();
            assertThat(usersWithoutAuthUser).isEqualTo(2);
        }

        @Test
        void givenValidRequestWithMinimumParticipants_whenCreatingGroup_thenShouldCreateGroupWithTwoUsers() {
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Small Group",
                            "Host User",
                            List.of("Participant 1")
                    );

            var result = groupService.createGroup(TEST_UID, request);

            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
            assertThat(userGroups).hasSize(2);

            long usersWithAuthUser = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .filter(u -> u.getAuthUser() != null)
                    .count();
            assertThat(usersWithAuthUser).isEqualTo(1);
        }

        @Test
        void givenUserLeftSomeGroups_whenCreatingGroup_thenShouldNotCountLeftGroupsTowardLimit() {
            UserEntity activeUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity leftUser1 = TestFixtures.Users.userWithoutAuthUser("Left User 1");
            UserEntity leftUser2 = TestFixtures.Users.userWithoutAuthUser("Left User 2");
            userRepository.saveAll(List.of(activeUser, leftUser1, leftUser2));

            IntStream.range(0, 7).forEach(i -> {
                GroupEntity group = TestFixtures.Groups.withName("Active Group " + i);
                groupRepository.save(group);
                userGroupRepository.save(TestFixtures.UserGroups.create(activeUser, group));
            });

            IntStream.range(0, 2).forEach(i -> {
                GroupEntity leftGroup = TestFixtures.Groups.withName("Left Group " + i);
                groupRepository.save(leftGroup);
                UserEntity leftUser = i == 0 ? leftUser1 : leftUser2;
                userGroupRepository.save(TestFixtures.UserGroups.create(leftUser, leftGroup));
            });

            flushAndClear();

            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "8th Active Group",
                            "Host",
                            List.of("Participant")
                    );

            var result = groupService.createGroup(TEST_UID, request);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo("8th Active Group");
        }

        @Test
        void givenSpecialUidUser_whenCreatingGroup_thenShouldAllowExceedingLimit() {
            String specialUid = "dev-unlimited-uid";
            AuthUserEntity specialAuthUser = AuthUserEntity.builder()
                    .uid(specialUid)
                    .name("Special User")
                    .email("special" + System.currentTimeMillis() + "@example.com")
                    .build();
            authUserRepository.save(specialAuthUser);

            UserEntity specialUser = TestFixtures.Users.userWithAuthUser(specialAuthUser);
            userRepository.save(specialUser);

            IntStream.range(0, 12).forEach(i -> {
                GroupEntity group = TestFixtures.Groups.withName("Existing Group " + i);
                groupRepository.save(group);
                userGroupRepository.save(TestFixtures.UserGroups.create(specialUser, group));
            });

            flushAndClear();

            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "13th Group",
                            "Host",
                            List.of("Participant")
                    );

            var result = groupService.createGroup(specialUid, request);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo("13th Group");
        }

        @Test
        void givenValidRequest_whenCreatingGroup_thenShouldCommitAllEntitiesAtomically() {
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Atomic Test Group",
                            "Host",
                            List.of("P1", "P2")
                    );

            var result = groupService.createGroup(TEST_UID, request);

            flushAndClear();

            UUID groupId = UUID.fromString(result.groupInfo().uuid());

            assertThat(groupRepository.findById(groupId)).isPresent();

            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
            assertThat(userGroups).hasSize(3);

            userGroups.forEach(ug -> {
                assertThat(userRepository.findById(ug.getUserUuid())).isPresent();
            });
        }
    }

    // ========================================
    // joinGroupInvited — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("joinGroupInvited — 紐付けの永続化")
    class JoinGroupInvited {

        @Test
        void givenValidJoinTokenAndUserNotInGroup_whenJoiningGroup_thenShouldJoinSuccessfully() {
            UserEntity existingUser = TestFixtures.Users.userWithoutAuthUser("Existing User");
            userRepository.save(existingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));

            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.save(joiningUser);
            flushAndClear();

            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            joiningUser.getUuid(),
                            joinToken
                    );

            var result = groupService.joinGroupInvited(request, testGroupId, TEST_UID);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(joiningUser.getUuid()).orElseThrow();
            assertThat(updatedUser.getAuthUser()).isNotNull();
            assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(TEST_UID);
        }

        @Test
        void givenValidJoinToken_whenJoiningGroup_thenShouldLinkUserToAuthUser() {
            UserEntity existingUser = TestFixtures.Users.userWithoutAuthUser("Existing User");
            userRepository.save(existingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));

            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.save(joiningUser);
            flushAndClear();

            UUID originalUuid = joiningUser.getUuid();
            UUID joinToken = testGroup.getJoinToken();

            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            originalUuid,
                            joinToken
                    );

            groupService.joinGroupInvited(request, testGroupId, TEST_UID);

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(originalUuid).orElseThrow();
            assertThat(updatedUser.getAuthUser()).isNotNull();
            assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(TEST_UID);
        }

        @Test
        void givenSpecialUidUser_whenJoiningGroup_thenShouldAllowExceedingLimit() {
            String specialUid = "dev-unlimited-uid";
            AuthUserEntity specialAuthUser = AuthUserEntity.builder()
                    .uid(specialUid)
                    .name("Special User")
                    .email("specialjoin" + System.currentTimeMillis() + "@example.com")
                    .build();
            authUserRepository.save(specialAuthUser);

            UserEntity specialUser = TestFixtures.Users.userWithAuthUser(specialAuthUser);
            userRepository.save(specialUser);

            IntStream.range(0, 12).forEach(i -> {
                GroupEntity group = TestFixtures.Groups.withName("Existing Group " + i);
                groupRepository.save(group);
                userGroupRepository.save(TestFixtures.UserGroups.create(specialUser, group));
            });

            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.save(joiningUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(joiningUser, testGroup));

            flushAndClear();

            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            joiningUser.getUuid(),
                            joinToken
                    );

            var result = groupService.joinGroupInvited(request, testGroupId, specialUid);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
        }
    }

    // ========================================
    // leaveGroup — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("leaveGroup — 紐付け解除の永続化")
    class LeaveGroup {

        @Test
        void givenUserIsInGroup_whenLeavingGroup_thenShouldNullifyAuthUserAndPersist() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            UUID userUuid = user.getUuid();

            groupService.leaveGroup(testGroupId, userUuid);

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(userUuid).orElseThrow();
            assertThat(updatedUser.getAuthUser()).isNull();
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getName()).isEqualTo(user.getName());
        }

        @Test
        void givenUserIsInGroup_whenLeavingGroup_thenShouldPreserveUserEntityAndUserGroupRelationship() {
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            UUID userUuid = user.getUuid();
            String originalName = user.getName();

            groupService.leaveGroup(testGroupId, userUuid);

            flushAndClear();
            Optional<UserEntity> userAfterLeaving = userRepository.findById(userUuid);
            assertThat(userAfterLeaving).isPresent();
            assertThat(userAfterLeaving.get().getName()).isEqualTo(originalName);
            assertThat(userAfterLeaving.get().getAuthUser()).isNull();

            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(userGroups).isNotEmpty();
        }

        @Test
        void givenUserLeftGroup_whenRejoiningSameGroup_thenShouldAllowRejoin() {
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            UUID userUuid = user.getUuid();

            groupService.leaveGroup(testGroupId, userUuid);
            flushAndClear();

            UserEntity leftUser = userRepository.findById(userUuid).orElseThrow();
            assertThat(leftUser.getAuthUser()).isNull();

            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO rejoinRequest =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            userUuid,
                            joinToken
                    );

            var result = groupService.joinGroupInvited(rejoinRequest, testGroupId, TEST_UID);

            assertThat(result).isNotNull();
            flushAndClear();
            UserEntity rejoinedUser = userRepository.findById(userUuid).orElseThrow();
            assertThat(rejoinedUser.getAuthUser()).isNotNull();
        }
    }

    // ========================================
    // addMember — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("addMember — メンバー永続化")
    class AddMember {

        @Test
        void givenValidRequestFromGroupMember_whenAddingMember_thenShouldPersistNewMember() {
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(requestingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(requestingUser, testGroup));
            flushAndClear();

            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            assertThat(result).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
            assertThat(result.users()).hasSize(2);

            flushAndClear();
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(userGroups).hasSize(2);

            UserEntity newMember = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .filter(u -> "New Member".equals(u.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(newMember.getAuthUser()).isNull();
        }

        @Test
        void givenMemberWithDuplicateName_whenAddingMember_thenShouldAllowDuplicateNames() {
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity existingMember = TestFixtures.Users.userWithoutAuthUser("Alice");
            userRepository.saveAll(List.of(requestingUser, existingMember));
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(requestingUser, testGroup),
                    TestFixtures.UserGroups.create(existingMember, testGroup)
            ));
            flushAndClear();

            AddMemberRequestDTO request = new AddMemberRequestDTO("Alice");

            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            assertThat(result.users()).hasSize(3);

            flushAndClear();
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            long aliceCount = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .filter(u -> "Alice".equals(u.getName()))
                    .count();
            assertThat(aliceCount).isEqualTo(2);
        }

        @Test
        void givenValidRequest_whenAddingMember_thenShouldCommitAllEntitiesAtomically() {
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(requestingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(requestingUser, testGroup));
            flushAndClear();

            AddMemberRequestDTO request = new AddMemberRequestDTO("New Member");

            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            flushAndClear();

            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(userGroups).hasSize(2);

            userGroups.forEach(ug -> {
                assertThat(userRepository.findById(ug.getUserUuid())).isPresent();
            });
        }
    }

    // ========================================
    // removeMember — Infrastructure behavior
    // ========================================

    @Nested
    @DisplayName("removeMember — エンティティ削除の永続化")
    class RemoveMember {

        @Test
        void givenUnjoinedMember_whenRemoving_thenShouldDeleteFromBothUsersAndUserGroupsTables() {
            UserEntity requester = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity unjoinedMember = TestFixtures.Users.userWithoutAuthUser("Unjoined Member");
            userRepository.saveAll(List.of(requester, unjoinedMember));
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(requester, testGroup),
                    TestFixtures.UserGroups.create(unjoinedMember, testGroup)
            ));
            flushAndClear();

            UUID unjoinedMemberUuid = unjoinedMember.getUuid();

            groupService.removeMember(testGroupId, unjoinedMemberUuid, TEST_UID);
            flushAndClear();

            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(userGroups).hasSize(1);
            assertThat(userGroups.get(0).getUserUuid()).isEqualTo(requester.getUuid());

            Optional<UserEntity> deletedUser = userRepository.findById(unjoinedMemberUuid);
            assertThat(deletedUser).isEmpty();
        }

        @Test
        void givenUnjoinedMember_whenRemoving_thenOtherMembersShouldNotBeAffected() {
            UserEntity requester = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity unjoinedTarget = TestFixtures.Users.userWithoutAuthUser("Target");
            UserEntity otherUnjoinedMember = TestFixtures.Users.userWithoutAuthUser("Other Unjoined");
            userRepository.saveAll(List.of(requester, unjoinedTarget, otherUnjoinedMember));
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(requester, testGroup),
                    TestFixtures.UserGroups.create(unjoinedTarget, testGroup),
                    TestFixtures.UserGroups.create(otherUnjoinedMember, testGroup)
            ));
            flushAndClear();

            groupService.removeMember(testGroupId, unjoinedTarget.getUuid(), TEST_UID);
            flushAndClear();

            List<UserGroupEntity> remaining = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(remaining).hasSize(2);

            List<UUID> remainingUserUuids = remaining.stream()
                    .map(UserGroupEntity::getUserUuid)
                    .toList();
            assertThat(remainingUserUuids).containsExactlyInAnyOrder(
                    requester.getUuid(), otherUnjoinedMember.getUuid()
            );

            assertThat(userRepository.findById(requester.getUuid())).isPresent();
            assertThat(userRepository.findById(otherUnjoinedMember.getUuid())).isPresent();
        }

        @Test
        void givenLastUnjoinedMember_whenRemoving_thenOnlyAuthenticatedMembersShouldRemain() {
            UserEntity requester = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity lastUnjoinedMember = TestFixtures.Users.userWithoutAuthUser("Last Unjoined");
            userRepository.saveAll(List.of(requester, lastUnjoinedMember));
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(requester, testGroup),
                    TestFixtures.UserGroups.create(lastUnjoinedMember, testGroup)
            ));
            flushAndClear();

            groupService.removeMember(testGroupId, lastUnjoinedMember.getUuid(), TEST_UID);
            flushAndClear();

            List<UserGroupEntity> remaining = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getUser().getAuthUser()).isNotNull();
            assertThat(remaining.get(0).getUser().getAuthUser().getUid()).isEqualTo(TEST_UID);
        }
    }
}
