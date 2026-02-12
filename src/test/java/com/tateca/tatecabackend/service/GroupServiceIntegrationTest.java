package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.AddMemberRequestDTO;
import com.tateca.tatecabackend.repository.TransactionRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        // Create test auth user
        testAuthUser = AuthUserEntity.builder()
                .uid(TEST_UID)
                .name("Test User")
                .email("test" + System.currentTimeMillis() + "@example.com")
                .build();
        authUserRepository.save(testAuthUser);

        // Create test group
        testGroup = TestFixtures.Groups.defaultGroup();
        groupRepository.save(testGroup);
        testGroupId = testGroup.getUuid();

        flushAndClear();
    }

    // ========================================
    // getGroupInfo Tests
    // ========================================

    @Nested
    class GetGroupInfo {

        @Test
        void givenGroupExistsWithUsers_whenGettingGroupInfo_thenShouldReturnCompleteGroupInformation() {
            // Given
            UserEntity user1 = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity user2 = TestFixtures.Users.userWithoutAuthUser("User 2");
            userRepository.save(user1);
            userRepository.save(user2);

            UserGroupEntity ug1 = TestFixtures.UserGroups.create(user1, testGroup);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(user2, testGroup);
            userGroupRepository.save(ug1);
            userGroupRepository.save(ug2);

            flushAndClear();

            // When
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.groupInfo()).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
            assertThat(result.users()).hasSize(2);
            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        void givenGroupWithMixedUsers_whenGettingGroupInfo_thenShouldIncludeUsersWithAndWithoutAuthUser() {
            // Given
            UserEntity userWithAuth = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity userWithoutAuth = TestFixtures.Users.userWithoutAuthUser("Participant");
            userRepository.save(userWithAuth);
            userRepository.save(userWithoutAuth);

            userGroupRepository.save(TestFixtures.UserGroups.create(userWithAuth, testGroup));
            userGroupRepository.save(TestFixtures.UserGroups.create(userWithoutAuth, testGroup));

            flushAndClear();

            // When
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then
            assertThat(result.users()).hasSize(2);
        }

        @Test
        void givenGroupWithUser_whenGettingGroupInfo_thenShouldCountTransactionsCorrectly() {
            // Given
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));

            flushAndClear();

            // When
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then
            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        void givenGroupWithNoUsers_whenGettingGroupInfo_thenShouldThrowNotFoundException() {
            // Given: Group with no users (already set up in setUp())

            // When & Then
            assertThatThrownBy(() -> groupService.getGroupInfo(testGroupId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }

        @Test
        void givenGroupDoesNotExist_whenGettingGroupInfo_thenShouldThrowNotFoundException() {
            // Given
            UUID nonExistentGroupId = UUID.randomUUID();

            // When & Then
            assertThatThrownBy(() -> groupService.getGroupInfo(nonExistentGroupId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }

        @Test
        void givenGroupWith20Users_whenGettingGroupInfo_thenShouldHandleLargeNumberOfUsers() {
            // Given
            List<UserEntity> users = IntStream.range(0, 20)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());
            userRepository.saveAll(users);

            List<UserGroupEntity> userGroups = users.stream()
                    .map(user -> TestFixtures.UserGroups.create(user, testGroup))
                    .collect(Collectors.toList());
            userGroupRepository.saveAll(userGroups);

            flushAndClear();

            // When
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then
            assertThat(result.users()).hasSize(20);
        }
    }

    // ========================================
    // updateGroupName Tests
    // ========================================

    @Nested
    class UpdateGroupName {

        @Test
        void givenGroupExists_whenUpdatingGroupName_thenShouldUpdateAndPersistToDatabase() {
            // Given
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            String newName = "Updated Group Name";

            // When
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, newName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo(newName);

            flushAndClear();
            Optional<GroupEntity> updatedGroup = groupRepository.findById(testGroupId);
            assertThat(updatedGroup).isPresent();
            assertThat(updatedGroup.get().getName()).isEqualTo(newName);
        }

        @Test
        void givenGroupExists_whenUpdatingGroupName_thenShouldUpdateUpdatedAtTimestamp() {
            // Given
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            GroupEntity originalGroup = groupRepository.findById(testGroupId).orElseThrow();
            java.time.Instant originalUpdatedAt = originalGroup.getUpdatedAt();

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            groupService.updateGroupName(testGroupId, "New Name");
            flushAndClear();

            // Then
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }

        @Test
        void givenGroupExists_whenUpdatingGroupName_thenShouldPreserveOtherGroupFields() {
            // Given
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            GroupEntity originalGroup = groupRepository.findById(testGroupId).orElseThrow();
            UUID originalJoinToken = originalGroup.getJoinToken();
            UUID originalUuid = originalGroup.getUuid();
            java.time.Instant originalCreatedAt = originalGroup.getCreatedAt();

            // When
            groupService.updateGroupName(testGroupId, "New Name");
            flushAndClear();

            // Then
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getJoinToken()).isEqualTo(originalJoinToken);
            assertThat(updatedGroup.getUuid()).isEqualTo(originalUuid);
            assertThat(updatedGroup.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                    .isEqualTo(originalCreatedAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        void givenGroupExists_whenUpdatingWithSpecialCharacters_thenShouldHandleSpecialCharactersInName() {
            // Given
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            String specialName = "Test 😊 田中 €$";

            // When
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, specialName);

            // Then
            assertThat(result.groupInfo().name()).isEqualTo(specialName);

            flushAndClear();
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getName()).isEqualTo(specialName);
        }

        @Test
        void givenGroupExists_whenUpdatingMultipleTimes_thenShouldHandleMultipleSequentialUpdates() {
            // Given
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            // When
            groupService.updateGroupName(testGroupId, "Name 1");
            flushAndClear();
            groupService.updateGroupName(testGroupId, "Name 2");
            flushAndClear();
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, "Name 3");
            flushAndClear();

            // Then
            assertThat(result.groupInfo().name()).isEqualTo("Name 3");
            GroupEntity finalGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(finalGroup.getName()).isEqualTo("Name 3");
        }

        @Test
        void givenGroupDoesNotExist_whenUpdatingGroupName_thenShouldThrowNotFoundException() {
            // Given
            UUID nonExistentGroupId = UUID.randomUUID();

            // When & Then
            assertThatThrownBy(() -> groupService.updateGroupName(nonExistentGroupId, "New Name"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }

        @Test
        void givenGroupWithMultipleUsers_whenUpdatingGroupName_thenShouldReturnCompleteInfoAfterUpdate() {
            // Given
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

            // When
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, "Updated Name");

            // Then
            assertThat(result.users()).hasSize(3);
            assertThat(result.groupInfo().name()).isEqualTo("Updated Name");
        }
    }

    // ========================================
    // getGroupList Tests (3 nested classes)
    // ========================================

    @Nested
    class GetGroupList {

        @Test
        void givenUserBelongsToMultipleGroups_whenGettingGroupList_thenShouldReturnAllGroups() {
            // Given: User in 5 groups
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

            // When: Getting group list
            var result = groupService.getGroupList(TEST_UID);

            // Then: Should return all 5 groups
            assertThat(result.groupList()).hasSize(5);
        }

        @Test
        void givenUserInMultipleGroupsWithDifferentRoles_whenGettingGroupList_thenShouldReturnAllGroups() {
            // Given: User as host in 2 groups and participant in 2 others
            UserEntity hostUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(hostUser);

            // Groups where user is host
            GroupEntity group1 = TestFixtures.Groups.withName("Hosted Group 1");
            GroupEntity group2 = TestFixtures.Groups.withName("Hosted Group 2");
            groupRepository.saveAll(List.of(group1, group2));

            // Groups where user is participant
            GroupEntity group3 = TestFixtures.Groups.withName("Participant Group 1");
            GroupEntity group4 = TestFixtures.Groups.withName("Participant Group 2");
            groupRepository.saveAll(List.of(group3, group4));

            // Add user to all groups
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(hostUser, group1),
                    TestFixtures.UserGroups.create(hostUser, group2),
                    TestFixtures.UserGroups.create(hostUser, group3),
                    TestFixtures.UserGroups.create(hostUser, group4)
            ));

            flushAndClear();

            // When: Getting group list
            var result = groupService.getGroupList(TEST_UID);

            // Then: Should return all 4 groups regardless of role
            assertThat(result.groupList()).hasSize(4);
        }

        @Test
        void givenUserLeftSomeGroups_whenGettingGroupList_thenShouldNotReturnLeftGroups() {
            // Given: User in 3 groups, but left 1 (authUser nullified)
            UserEntity activeUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity leftUser = TestFixtures.Users.userWithoutAuthUser("Left User");
            userRepository.saveAll(List.of(activeUser, leftUser));

            GroupEntity activeGroup1 = TestFixtures.Groups.withName("Active Group 1");
            GroupEntity activeGroup2 = TestFixtures.Groups.withName("Active Group 2");
            GroupEntity leftGroup = TestFixtures.Groups.withName("Left Group");
            groupRepository.saveAll(List.of(activeGroup1, activeGroup2, leftGroup));

            // Active memberships
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(activeUser, activeGroup1),
                    TestFixtures.UserGroups.create(activeUser, activeGroup2),
                    TestFixtures.UserGroups.create(leftUser, leftGroup)
            ));

            flushAndClear();

            // When: Getting group list
            var result = groupService.getGroupList(TEST_UID);

            // Then: Should return only active groups (2)
            assertThat(result.groupList()).hasSize(2);
        }

        @Test
        void givenUserBelongsToNoGroups_whenGettingGroupList_thenShouldReturnEmptyList() {
            // Given: User exists but has no group memberships
            // (testAuthUser already exists from setUp)

            // When: Getting group list
            var result = groupService.getGroupList(TEST_UID);

            // Then: Should return empty list
            assertThat(result.groupList()).isEmpty();
        }

        @Test
        void givenUserDoesNotExist_whenGettingGroupList_thenShouldReturnEmptyList() {
            // Given: Non-existent UID
            String nonExistentUid = "non-existent-uid-" + System.currentTimeMillis();

            // When: Getting group list
            var result = groupService.getGroupList(nonExistentUid);

            // Then: Should return empty list (no exception)
            assertThat(result.groupList()).isEmpty();
        }
    }

    // ========================================
    // createGroup Tests
    // ========================================

    @Nested
    class CreateGroup {

        @Test
        void givenValidRequestWithHostAndParticipants_whenCreatingGroup_thenShouldPersistAllEntities() {
            // Given: Valid request with 3 participants
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Integration Test Group",
                            "Host User",
                            List.of("Participant 1", "Participant 2", "Participant 3")
                    );

            // When: Creating group
            var result = groupService.createGroup(TEST_UID, request);

            // Then: Should return group response
            assertThat(result).isNotNull();
            assertThat(result.groupInfo()).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo("Integration Test Group");

            // And: Group should be persisted
            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            Optional<GroupEntity> savedGroup = groupRepository.findById(groupId);
            assertThat(savedGroup).isPresent();

            // And: All 4 users should be created (1 host + 3 participants)
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
            assertThat(userGroups).hasSize(4);

            // And: Transaction count should be 0
            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        void givenValidRequest_whenCreatingGroup_thenShouldGenerateUniqueJoinToken() {
            // Given: Valid request
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Test Group",
                            "Host",
                            List.of("Participant")
                    );

            // When: Creating group
            var result = groupService.createGroup(TEST_UID, request);

            // Then: Join token should be generated
            assertThat(result.groupInfo().joinToken()).isNotNull();

            // And: Should be valid UUID format
            assertThat(UUID.fromString(result.groupInfo().joinToken())).isNotNull();

            // And: Should be persisted
            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            GroupEntity savedGroup = groupRepository.findById(groupId).orElseThrow();
            assertThat(savedGroup.getJoinToken()).isNotNull();
        }

        @Test
        void givenValidRequest_whenCreatingGroup_thenShouldSetTimestamps() {
            // Given: Valid request
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Timestamp Test Group",
                            "Host",
                            List.of("Participant")
                    );

            java.time.Instant beforeCreate = java.time.Instant.now();

            // When: Creating group
            var result = groupService.createGroup(TEST_UID, request);

            java.time.Instant afterCreate = java.time.Instant.now();

            // Then: Timestamps should be set
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
            // Given: Request with host and participants
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Auth Link Test Group",
                            "Host User",
                            List.of("Participant 1", "Participant 2")
                    );

            // When: Creating group
            var result = groupService.createGroup(TEST_UID, request);

            // Then: Host should be linked to authUser
            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);

            List<UserEntity> users = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .toList();

            // Host (first user) should have authUser
            long usersWithAuthUser = users.stream()
                    .filter(u -> u.getAuthUser() != null)
                    .count();
            assertThat(usersWithAuthUser).isEqualTo(1);

            // Participants should not have authUser
            long usersWithoutAuthUser = users.stream()
                    .filter(u -> u.getAuthUser() == null)
                    .count();
            assertThat(usersWithoutAuthUser).isEqualTo(2);
        }

        @Test
        void givenValidRequestWithMinimumParticipants_whenCreatingGroup_thenShouldCreateGroupWithTwoUsers() {
            // Given: Request with minimum 1 participant
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Small Group",
                            "Host User",
                            List.of("Participant 1")
                    );

            // When: Creating group
            var result = groupService.createGroup(TEST_UID, request);

            // Then: 2 users should be created (1 host + 1 participant)
            flushAndClear();
            UUID groupId = UUID.fromString(result.groupInfo().uuid());
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
            assertThat(userGroups).hasSize(2);

            // And: Host should have authUser
            long usersWithAuthUser = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .filter(u -> u.getAuthUser() != null)
                    .count();
            assertThat(usersWithAuthUser).isEqualTo(1);
        }

        @Test
        void givenUserAtMaxGroupLimit_whenCreatingGroup_thenShouldThrowConflictException() {
            // Given: User already in 9 groups (one UserEntity per group with authUser)
            IntStream.range(0, 9).forEach(i -> {
                GroupEntity group = TestFixtures.Groups.withName("Existing Group " + i);
                groupRepository.save(group);

                UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
                userRepository.save(user);

                userGroupRepository.save(TestFixtures.UserGroups.create(user, group));
            });

            flushAndClear();

            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "10th Group",
                            "Host",
                            List.of("Participant")
                    );

            // When & Then: Should throw conflict exception
            assertThatThrownBy(() -> groupService.createGroup(TEST_UID, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("User can't join more than 10 groups");
        }

        @Test
        void givenUserLeftSomeGroups_whenCreatingGroup_thenShouldNotCountLeftGroupsTowardLimit() {
            // Given: User in 9 groups, but left 2 (authUser nullified)
            UserEntity activeUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity leftUser1 = TestFixtures.Users.userWithoutAuthUser("Left User 1");
            UserEntity leftUser2 = TestFixtures.Users.userWithoutAuthUser("Left User 2");
            userRepository.saveAll(List.of(activeUser, leftUser1, leftUser2));

            // Create 7 active groups
            IntStream.range(0, 7).forEach(i -> {
                GroupEntity group = TestFixtures.Groups.withName("Active Group " + i);
                groupRepository.save(group);
                userGroupRepository.save(TestFixtures.UserGroups.create(activeUser, group));
            });

            // Create 2 left groups
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

            // When: Creating group (should succeed since only 7 active)
            var result = groupService.createGroup(TEST_UID, request);

            // Then: Should succeed
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo("8th Active Group");
        }

        @Test
        void givenSpecialUidUser_whenCreatingGroup_thenShouldAllowExceedingLimit() {
            // Given: Special UID with 12 existing groups
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

            // When: Creating group with special UID
            var result = groupService.createGroup(specialUid, request);

            // Then: Should succeed
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo("13th Group");
        }

        @Test
        void givenAuthUserDoesNotExist_whenCreatingGroup_thenShouldThrowNotFoundException() {
            // Given: Non-existent auth user UID
            String nonExistentUid = "non-existent-auth-user-" + System.currentTimeMillis();

            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Test Group",
                            "Host",
                            List.of("Participant")
                    );

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.createGroup(nonExistentUid, request))
                    .hasMessageContaining("Auth user not found");
        }

        @Test
        void givenValidRequest_whenCreatingGroup_thenShouldCommitAllEntitiesAtomically() {
            // Given: Valid request with participants
            com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO(
                            "Atomic Test Group",
                            "Host",
                            List.of("P1", "P2")
                    );

            // When: Creating group
            var result = groupService.createGroup(TEST_UID, request);

            // Then: All entities should be committed together
            flushAndClear();

            UUID groupId = UUID.fromString(result.groupInfo().uuid());

            // Group should exist
            assertThat(groupRepository.findById(groupId)).isPresent();

            // All users should exist
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
            assertThat(userGroups).hasSize(3);

            // All user entities should be persisted
            userGroups.forEach(ug -> {
                assertThat(userRepository.findById(ug.getUserUuid())).isPresent();
            });
        }
    }

    // ========================================
    // joinGroupInvited Tests
    // ========================================

    @Nested
    class JoinGroupInvited {

        @Test
        void givenValidJoinTokenAndUserNotInGroup_whenJoiningGroup_thenShouldJoinSuccessfully() {
            // Given: Group with existing users
            UserEntity existingUser = TestFixtures.Users.userWithoutAuthUser("Existing User");
            userRepository.save(existingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));

            // Create another user (participant) who will join
            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.save(joiningUser);
            flushAndClear();

            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            joiningUser.getUuid(),
                            joinToken
                    );

            // When: Joining group
            var result = groupService.joinGroupInvited(request, testGroupId, TEST_UID);

            // Then: Should return group info
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());

            // And: User should be linked to authUser
            flushAndClear();
            UserEntity updatedUser = userRepository.findById(joiningUser.getUuid()).orElseThrow();
            assertThat(updatedUser.getAuthUser()).isNotNull();
            assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(TEST_UID);
        }

        @Test
        void givenValidJoinToken_whenJoiningGroup_thenShouldLinkUserToAuthUser() {
            // Given: Group and user without authUser
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

            // When: Joining group
            groupService.joinGroupInvited(request, testGroupId, TEST_UID);

            // Then: Should link correctly in database
            flushAndClear();
            UserEntity updatedUser = userRepository.findById(originalUuid).orElseThrow();
            assertThat(updatedUser.getAuthUser()).isNotNull();
            assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(TEST_UID);
        }

        @Test
        void givenInvalidJoinToken_whenJoiningGroup_thenShouldThrowForbiddenException() {
            // Given: Invalid join token
            UserEntity existingUser = TestFixtures.Users.userWithoutAuthUser("Existing User");
            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.saveAll(List.of(existingUser, joiningUser));
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));
            flushAndClear();

            UUID wrongToken = UUID.randomUUID();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            joiningUser.getUuid(),
                            wrongToken
                    );

            // When & Then: Should throw forbidden exception
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Invalid or expired join token");

            // And: User should not be modified
            flushAndClear();
            UserEntity unchangedUser = userRepository.findById(joiningUser.getUuid()).orElseThrow();
            assertThat(unchangedUser.getAuthUser()).isNull();
        }

        @Test
        void givenUserAlreadyInGroup_whenJoiningGroup_thenShouldThrowConflictException() {
            // Given: User already in group
            UserEntity existingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(existingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));
            flushAndClear();

            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            existingUser.getUuid(),
                            joinToken
                    );

            // When & Then: Should throw conflict exception
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("You have already joined this group");
        }

        @Test
        void givenDifferentUserEntityWithSameAuthUser_whenJoiningGroup_thenShouldDetectDuplicate() {
            // Given: Different user entity but same authUser UID
            UserEntity existingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity anotherUser = TestFixtures.Users.userWithoutAuthUser("Another User");
            userRepository.saveAll(List.of(existingUser, anotherUser));
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));
            flushAndClear();

            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            anotherUser.getUuid(),
                            joinToken
                    );

            // When & Then: Should detect duplicate by authUser UID
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("You have already joined this group");
        }

        @Test
        void givenUserAtMaxGroupLimit_whenJoiningGroup_thenShouldThrowConflictException() {
            // Given: User already in 9 groups (one UserEntity per group with authUser)
            IntStream.range(0, 9).forEach(i -> {
                GroupEntity group = TestFixtures.Groups.withName("Existing Group " + i);
                groupRepository.save(group);

                UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
                userRepository.save(user);

                userGroupRepository.save(TestFixtures.UserGroups.create(user, group));
            });

            // Create a participant in testGroup who will try to join
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

            // When & Then: Should throw conflict exception
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("User can't join more than 10 groups");
        }

        @Test
        void givenSpecialUidUser_whenJoiningGroup_thenShouldAllowExceedingLimit() {
            // Given: Special UID with 12 existing groups
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

            // Create a participant in testGroup who will try to join
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

            // When: Joining group with special UID
            var result = groupService.joinGroupInvited(request, testGroupId, specialUid);

            // Then: Should succeed
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
        }

        @Test
        void givenUserDoesNotExist_whenJoiningGroup_thenShouldThrowNotFoundException() {
            // Given: Non-existent user UUID
            UserEntity existingUser = TestFixtures.Users.userWithoutAuthUser("Existing User");
            userRepository.save(existingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));
            flushAndClear();

            UUID nonExistentUserUuid = UUID.randomUUID();
            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            nonExistentUserUuid,
                            joinToken
                    );

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, TEST_UID))
                    .hasMessageContaining("User not found");
        }

        @Test
        void givenAuthUserDoesNotExist_whenJoiningGroup_thenShouldThrowNotFoundException() {
            // Given: Non-existent auth user UID
            UserEntity existingUser = TestFixtures.Users.userWithoutAuthUser("Existing User");
            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.saveAll(List.of(existingUser, joiningUser));
            userGroupRepository.save(TestFixtures.UserGroups.create(existingUser, testGroup));
            flushAndClear();

            String nonExistentUid = "non-existent-uid-" + System.currentTimeMillis();
            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            joiningUser.getUuid(),
                            joinToken
                    );

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, testGroupId, nonExistentUid))
                    .hasMessageContaining("Auth user not found");
        }

        @Test
        void givenGroupDoesNotExist_whenJoiningGroup_thenShouldThrowNotFoundException() {
            // Given: Non-existent group ID
            UUID nonExistentGroupId = UUID.randomUUID();
            UserEntity joiningUser = TestFixtures.Users.userWithoutAuthUser("Joining User");
            userRepository.save(joiningUser);
            flushAndClear();

            UUID joinToken = UUID.randomUUID();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO request =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            joiningUser.getUuid(),
                            joinToken
                    );

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.joinGroupInvited(request, nonExistentGroupId, TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }
    }

    // ========================================
    // leaveGroup Tests
    // ========================================

    @Nested
    class LeaveGroup {

        @Test
        void givenUserIsInGroup_whenLeavingGroup_thenShouldLeaveSuccessfully() {
            // Given: User in group
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            UUID userUuid = user.getUuid();

            // When: Leaving group
            groupService.leaveGroup(testGroupId, userUuid);

            // Then: User's authUser should be nullified
            flushAndClear();
            UserEntity updatedUser = userRepository.findById(userUuid).orElseThrow();
            assertThat(updatedUser.getAuthUser()).isNull();

            // And: User entity should still exist
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getName()).isEqualTo(user.getName());
        }

        @Test
        void givenUserIsInGroup_whenLeavingGroup_thenShouldPreserveUserEntity() {
            // Given: User in group
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            UUID userUuid = user.getUuid();
            String originalName = user.getName();

            // When: Leaving group
            groupService.leaveGroup(testGroupId, userUuid);

            // Then: User entity should still exist
            flushAndClear();
            Optional<UserEntity> userAfterLeaving = userRepository.findById(userUuid);
            assertThat(userAfterLeaving).isPresent();
            assertThat(userAfterLeaving.get().getName()).isEqualTo(originalName);
            assertThat(userAfterLeaving.get().getAuthUser()).isNull();

            // And: UserGroup relationship should still exist
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(userGroups).isNotEmpty();
        }

        @Test
        void givenUserLeftGroup_whenRejoiningSameGroup_thenShouldAllowRejoin() {
            // Given: User leaves group
            UserEntity user = TestFixtures.Users.userWithoutAuthUser("User");
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            UUID userUuid = user.getUuid();

            // When: User leaves
            groupService.leaveGroup(testGroupId, userUuid);
            flushAndClear();

            // Verify user left (authUser is null)
            UserEntity leftUser = userRepository.findById(userUuid).orElseThrow();
            assertThat(leftUser.getAuthUser()).isNull();

            // Then: User should be able to rejoin
            UUID joinToken = testGroup.getJoinToken();
            com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO rejoinRequest =
                    new com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO(
                            userUuid,
                            joinToken
                    );

            var result = groupService.joinGroupInvited(rejoinRequest, testGroupId, TEST_UID);

            // And: Should be back in group
            assertThat(result).isNotNull();
            flushAndClear();
            UserEntity rejoinedUser = userRepository.findById(userUuid).orElseThrow();
            assertThat(rejoinedUser.getAuthUser()).isNotNull();
        }

        @Test
        void givenUserNotInGroup_whenLeavingGroup_thenShouldThrowNotFoundException() {
            // Given: User exists but not in the group
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            flushAndClear();

            UUID userUuid = user.getUuid();

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, userUuid))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User is not in this group");
        }

        @Test
        void givenGroupDoesNotExist_whenLeavingGroup_thenShouldThrowNotFoundException() {
            // Given: Non-existent group ID
            UUID nonExistentGroupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.leaveGroup(nonExistentGroupId, userUuid))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }

        @Test
        void givenUserDoesNotExist_whenLeavingGroup_thenShouldThrowNotFoundException() {
            // Given: Non-existent user UUID
            UUID nonExistentUserUuid = UUID.randomUUID();

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, nonExistentUserUuid))
                    .hasMessageContaining("User is not in this group");
        }
    }

    // ========================================
    // addMember Tests
    // ========================================

    @Nested
    class AddMember {

        @Test
        void givenValidRequestFromGroupMember_whenAddingMember_thenShouldAddSuccessfully() {
            // Given: User in group
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(requestingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(requestingUser, testGroup));
            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("New Member");

            // When: Adding member
            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            // Then: Should return updated group info
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
            assertThat(result.users()).hasSize(2); // requesting user + new member

            // And: New member should be persisted without authUser
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
            // Given: Group with existing member named "Alice"
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity existingMember = TestFixtures.Users.userWithoutAuthUser("Alice");
            userRepository.saveAll(List.of(requestingUser, existingMember));
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(requestingUser, testGroup),
                    TestFixtures.UserGroups.create(existingMember, testGroup)
            ));
            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("Alice");

            // When: Adding another member with same name
            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            // Then: Should succeed (duplicate names allowed)
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
        void givenGroupWithExistingMembers_whenAddingMember_thenShouldIncrementMemberCount() {
            // Given: Group with 3 existing members
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity member1 = TestFixtures.Users.userWithoutAuthUser("Member 1");
            UserEntity member2 = TestFixtures.Users.userWithoutAuthUser("Member 2");
            userRepository.saveAll(List.of(requestingUser, member1, member2));
            userGroupRepository.saveAll(List.of(
                    TestFixtures.UserGroups.create(requestingUser, testGroup),
                    TestFixtures.UserGroups.create(member1, testGroup),
                    TestFixtures.UserGroups.create(member2, testGroup)
            ));
            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("Member 4");

            // When: Adding member
            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            // Then: Member count should be 4
            assertThat(result.users()).hasSize(4);
        }

        @Test
        void givenUserIsNotGroupMember_whenAddingMember_thenShouldThrowForbiddenException() {
            // Given: User exists but not in the group
            UserEntity nonMember = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity existingMember = TestFixtures.Users.userWithoutAuthUser("Existing Member");
            userRepository.saveAll(List.of(nonMember, existingMember));
            userGroupRepository.save(TestFixtures.UserGroups.create(existingMember, testGroup));
            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("New Member");

            // When & Then: Should throw forbidden exception
            assertThatThrownBy(() -> groupService.addMember(testGroupId, TEST_UID, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only group members can add members");
        }

        @Test
        void givenGroupHasReachedMaximumSize_whenAddingMember_thenShouldThrowConflictException() {
            // Given: Group with 10 members (maximum)
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(requestingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(requestingUser, testGroup));

            IntStream.range(0, 9).forEach(i -> {
                UserEntity member = TestFixtures.Users.userWithoutAuthUser("Member " + i);
                userRepository.save(member);
                userGroupRepository.save(TestFixtures.UserGroups.create(member, testGroup));
            });

            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("11th Member");

            // When & Then: Should throw conflict exception
            assertThatThrownBy(() -> groupService.addMember(testGroupId, TEST_UID, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Group has reached maximum size of 10 members");
        }

        @Test
        void givenGroupUnderMaximumSize_whenAddingMember_thenShouldAllowAdding() {
            // Given: Group with 9 members (one under maximum)
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(requestingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(requestingUser, testGroup));

            IntStream.range(0, 8).forEach(i -> {
                UserEntity member = TestFixtures.Users.userWithoutAuthUser("Member " + i);
                userRepository.save(member);
                userGroupRepository.save(TestFixtures.UserGroups.create(member, testGroup));
            });

            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("10th Member");

            // When: Adding member
            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            // Then: Should succeed
            assertThat(result).isNotNull();
            assertThat(result.users()).hasSize(10);
        }

        @Test
        void givenGroupDoesNotExist_whenAddingMember_thenShouldThrowNotFoundException() {
            // Given: Non-existent group ID
            UUID nonExistentGroupId = UUID.randomUUID();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("New Member");

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.addMember(nonExistentGroupId, TEST_UID, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }

        @Test
        void givenValidRequest_whenAddingMember_thenShouldCommitAllEntitiesAtomically() {
            // Given: User in group
            UserEntity requestingUser = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(requestingUser);
            userGroupRepository.save(TestFixtures.UserGroups.create(requestingUser, testGroup));
            flushAndClear();

            AddMemberRequestDTO request =
                    new AddMemberRequestDTO("New Member");

            // When: Adding member
            GroupResponseDTO result = groupService.addMember(testGroupId, TEST_UID, request);

            // Then: All entities should be committed together
            flushAndClear();

            // New user should exist
            List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(testGroupId);
            assertThat(userGroups).hasSize(2);

            // User entity should be persisted
            userGroups.forEach(ug -> {
                assertThat(userRepository.findById(ug.getUserUuid())).isPresent();
            });
        }
    }
}
