package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
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

@DisplayName("GroupService Integration Tests")
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
    // getGroupInfo Tests (4 nested classes)
    // ========================================

    @Nested
    @DisplayName("Given group exists with users")
    class WhenGroupExistsWithUsers {

        @Test
        @DisplayName("Then should return complete group information")
        void thenShouldReturnCompleteGroupInformation() {
            // Given: Group with users
            UserEntity user1 = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity user2 = TestFixtures.Users.userWithoutAuthUser("User 2");
            userRepository.save(user1);
            userRepository.save(user2);

            UserGroupEntity ug1 = TestFixtures.UserGroups.create(user1, testGroup);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(user2, testGroup);
            userGroupRepository.save(ug1);
            userGroupRepository.save(ug2);

            flushAndClear();

            // When: Getting group info
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then: Should return complete group information
            assertThat(result).isNotNull();
            assertThat(result.groupInfo()).isNotNull();
            assertThat(result.groupInfo().uuid()).isEqualTo(testGroupId.toString());
            assertThat(result.users()).hasSize(2);
            assertThat(result.transactionCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Then should include users with and without auth user")
        void thenShouldIncludeUsersWithAndWithoutAuthUser() {
            // Given: Group with mixed users
            UserEntity userWithAuth = TestFixtures.Users.userWithAuthUser(testAuthUser);
            UserEntity userWithoutAuth = TestFixtures.Users.userWithoutAuthUser("Participant");
            userRepository.save(userWithAuth);
            userRepository.save(userWithoutAuth);

            userGroupRepository.save(TestFixtures.UserGroups.create(userWithAuth, testGroup));
            userGroupRepository.save(TestFixtures.UserGroups.create(userWithoutAuth, testGroup));

            flushAndClear();

            // When: Getting group info
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then: Should include both types of users
            assertThat(result.users()).hasSize(2);
        }

        @Test
        @DisplayName("Then should count transactions correctly")
        void thenShouldCountTransactionsCorrectly() {
            // Given: Group with user (transactions count will be 0 from transactionAccessor)
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));

            flushAndClear();

            // When: Getting group info
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then: Transaction count should be 0
            assertThat(result.transactionCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Given group exists but has no users")
    class WhenGroupExistsButHasNoUsers {

        @Test
        @DisplayName("Then should throw not found exception when no users in group")
        void thenShouldThrowNotFoundExceptionWhenNoUsersInGroup() {
            // Given: Group with no users (already set up in setUp())

            // When & Then: Should throw exception
            assertThatThrownBy(() -> groupService.getGroupInfo(testGroupId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group Not Found");
        }
    }

    @Nested
    @DisplayName("Given group does not exist")
    class WhenGroupDoesNotExist {

        @Test
        @DisplayName("Then should throw not found exception when group not found")
        void thenShouldThrowNotFoundExceptionWhenGroupNotFound() {
            // Given: Non-existent group ID
            UUID nonExistentGroupId = UUID.randomUUID();

            // When & Then: Should throw exception
            assertThatThrownBy(() -> groupService.getGroupInfo(nonExistentGroupId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group Not Found");
        }
    }

    @Nested
    @DisplayName("Given multiple users in group")
    class WhenMultipleUsersInGroup {

        @Test
        @DisplayName("Then should handle large number of users")
        void thenShouldHandleLargeNumberOfUsers() {
            // Given: Group with 20 users
            List<UserEntity> users = IntStream.range(0, 20)
                    .mapToObj(i -> TestFixtures.Users.userWithoutAuthUser("User " + i))
                    .collect(Collectors.toList());
            userRepository.saveAll(users);

            List<UserGroupEntity> userGroups = users.stream()
                    .map(user -> TestFixtures.UserGroups.create(user, testGroup))
                    .collect(Collectors.toList());
            userGroupRepository.saveAll(userGroups);

            flushAndClear();

            // When: Getting group info
            GroupResponseDTO result = groupService.getGroupInfo(testGroupId);

            // Then: Should return all 20 users
            assertThat(result.users()).hasSize(20);
        }
    }

    // ========================================
    // updateGroupName Tests (3 nested classes)
    // ========================================

    @Nested
    @DisplayName("Given group exists")
    class WhenGroupExistsForUpdate {

        @Test
        @DisplayName("Then should update groupName and persist to database")
        void thenShouldUpdateNameAndPersistToDatabase() {
            // Given: Group with users
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            String newName = "Updated Group Name";

            // When: Updating group groupName
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, newName);

            // Then: Should return updated info
            assertThat(result).isNotNull();
            assertThat(result.groupInfo().name()).isEqualTo(newName);

            // And: Changes should be persisted in database
            flushAndClear();
            Optional<GroupEntity> updatedGroup = groupRepository.findById(testGroupId);
            assertThat(updatedGroup).isPresent();
            assertThat(updatedGroup.get().getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("Then should update updatedAt timestamp")
        void thenShouldUpdateUpdatedAtTimestamp() {
            // Given: Group with users
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            GroupEntity originalGroup = groupRepository.findById(testGroupId).orElseThrow();
            java.time.Instant originalUpdatedAt = originalGroup.getUpdatedAt();

            // Sleep briefly to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When: Updating group groupName
            groupService.updateGroupName(testGroupId, "New Name");
            flushAndClear();

            // Then: updatedAt should be updated
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }

        @Test
        @DisplayName("Then should preserve other group fields")
        void thenShouldPreserveOtherGroupFields() {
            // Given: Group with users
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            // Reload group from database to get persisted timestamps
            GroupEntity originalGroup = groupRepository.findById(testGroupId).orElseThrow();
            UUID originalJoinToken = originalGroup.getJoinToken();
            UUID originalUuid = originalGroup.getUuid();
            java.time.Instant originalCreatedAt = originalGroup.getCreatedAt();

            // When: Updating group groupName
            groupService.updateGroupName(testGroupId, "New Name");
            flushAndClear();

            // Then: Other fields should be preserved
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getJoinToken()).isEqualTo(originalJoinToken);
            assertThat(updatedGroup.getUuid()).isEqualTo(originalUuid);
            // MySQL truncates microseconds, so compare truncated to seconds
            assertThat(updatedGroup.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                    .isEqualTo(originalCreatedAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Then should handle special characters in groupName")
        void thenShouldHandleSpecialCharactersInName() {
            // Given: Group with users
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            String specialName = "Test 😊 田中 €$";

            // When: Updating with special characters
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, specialName);

            // Then: Should preserve special characters
            assertThat(result.groupInfo().name()).isEqualTo(specialName);

            flushAndClear();
            GroupEntity updatedGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(updatedGroup.getName()).isEqualTo(specialName);
        }

        @Test
        @DisplayName("Then should handle multiple sequential updates")
        void thenShouldHandleMultipleSequentialUpdates() {
            // Given: Group with users
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);
            userGroupRepository.save(TestFixtures.UserGroups.create(user, testGroup));
            flushAndClear();

            // When: Multiple updates
            groupService.updateGroupName(testGroupId, "Name 1");
            flushAndClear();
            groupService.updateGroupName(testGroupId, "Name 2");
            flushAndClear();
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, "Name 3");
            flushAndClear();

            // Then: Should have final groupName
            assertThat(result.groupInfo().name()).isEqualTo("Name 3");
            GroupEntity finalGroup = groupRepository.findById(testGroupId).orElseThrow();
            assertThat(finalGroup.getName()).isEqualTo("Name 3");
        }
    }

    @Nested
    @DisplayName("Given group does not exist for update")
    class WhenGroupDoesNotExistForUpdate {

        @Test
        @DisplayName("Then should throw not found exception when group not found")
        void thenShouldThrowNotFoundExceptionWhenGroupNotFound() {
            // Given: Non-existent group ID
            UUID nonExistentGroupId = UUID.randomUUID();

            // When & Then: Should throw exception
            assertThatThrownBy(() -> groupService.updateGroupName(nonExistentGroupId, "New Name"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }
    }

    @Nested
    @DisplayName("Given group has users and transactions for update")
    class WhenGroupHasUsersAndTransactionsForUpdate {

        @Test
        @DisplayName("Then should return complete info after update")
        void thenShouldReturnCompleteInfoAfterUpdate() {
            // Given: Group with multiple users
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

            // When: Updating group groupName
            GroupResponseDTO result = groupService.updateGroupName(testGroupId, "Updated Name");

            // Then: Should return all users
            assertThat(result.users()).hasSize(3);
            assertThat(result.groupInfo().name()).isEqualTo("Updated Name");
        }
    }

    // ========================================
    // getGroupList Tests (3 nested classes)
    // ========================================

    @Nested
    @DisplayName("Given user belongs to multiple groups")
    class WhenUserBelongsToMultipleGroups {

        @Test
        @DisplayName("Then should return all groups user belongs to")
        void thenShouldReturnAllGroupsUserBelongsTo() {
            // Given: User in 5 groups
            UserEntity user = TestFixtures.Users.userWithAuthUser(testAuthUser);
            userRepository.save(user);

            List<GroupEntity> groups = IntStream.range(0, 5)
                    .mapToObj(i -> {
                        GroupEntity group = TestFixtures.Groups.withName("Group " + i);
                        groupRepository.save(group);
                        return group;
                    })
                    .collect(Collectors.toList());

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
        @DisplayName("Then should handle user in multiple groups with different roles")
        void thenShouldHandleUserInMultipleGroupsWithDifferentRoles() {
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
        @DisplayName("Then should not return groups user left already")
        void thenShouldNotReturnGroupsUserLeftAlready() {
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
    }

    @Nested
    @DisplayName("Given user belongs to no groups")
    class WhenUserBelongsToNoGroups {

        @Test
        @DisplayName("Then should return empty list when user has no groups")
        void thenShouldReturnEmptyListWhenUserHasNoGroups() {
            // Given: User exists but has no group memberships
            // (testAuthUser already exists from setUp)

            // When: Getting group list
            var result = groupService.getGroupList(TEST_UID);

            // Then: Should return empty list
            assertThat(result.groupList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given user does not exist")
    class WhenUserDoesNotExist {

        @Test
        @DisplayName("Then should return empty list for non-existent user")
        void thenShouldReturnEmptyListForNonExistentUser() {
            // Given: Non-existent UID
            String nonExistentUid = "non-existent-uid-" + System.currentTimeMillis();

            // When: Getting group list
            var result = groupService.getGroupList(nonExistentUid);

            // Then: Should return empty list (no exception)
            assertThat(result.groupList()).isEmpty();
        }
    }

    // ========================================
    // createGroup Tests (6 nested classes)
    // ========================================

    @Nested
    @DisplayName("Given valid request with host and participants")
    class WhenValidRequestWithHostAndParticipants {

        @Test
        @DisplayName("Then should create group with all entities persisted")
        void thenShouldCreateGroupWithAllEntitiesPersisted() {
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
        @DisplayName("Then should generate unique join token")
        void thenShouldGenerateUniqueJoinToken() {
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
        @DisplayName("Then should set createdAt and updatedAt timestamps")
        void thenShouldSetCreatedAtAndUpdatedAtTimestamps() {
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
        @DisplayName("Then should link only host to auth user")
        void thenShouldLinkOnlyHostToAuthUser() {
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
                    .collect(Collectors.toList());

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
    }

    @Nested
    @DisplayName("Given valid request with minimum participants")
    class WhenValidRequestWithMinimumParticipants {

        @Test
        @DisplayName("Then should create group with host and one participant")
        void thenShouldCreateGroupWithHostAndOneParticipant() {
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
    }

    @Nested
    @DisplayName("Given user at max group limit")
    class WhenUserAtMaxGroupLimit {

        @Test
        @DisplayName("Then should throw conflict exception when at limit")
        void thenShouldThrowConflictExceptionWhenAtLimit() {
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
        @DisplayName("Then should not count left groups toward limit")
        void thenShouldNotCountLeftGroupsTowardLimit() {
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
    }

    @Nested
    @DisplayName("Given special UID user")
    class WhenSpecialUidUser {

        @Test
        @DisplayName("Then should allow special UID to exceed limit")
        void thenShouldAllowSpecialUidToExceedLimit() {
            // Given: Special UID with 12 existing groups
            String specialUid = "v6CGVApOmVM4VWTijmRTg8m01Kj1";
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
    }

    @Nested
    @DisplayName("Given auth user does not exist")
    class WhenAuthUserDoesNotExist {

        @Test
        @DisplayName("Then should throw not found exception when auth user not found")
        void thenShouldThrowNotFoundExceptionWhenAuthUserNotFound() {
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
    }

    @Nested
    @DisplayName("Given transaction boundaries")
    class WhenTransactionBoundaries {

        @Test
        @DisplayName("Then should commit all entities atomically")
        void thenShouldCommitAllEntitiesAtomically() {
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
    // joinGroupInvited Tests (7 nested classes)
    // ========================================

    @Nested
    @DisplayName("Given valid join token and user not in group")
    class WhenValidJoinTokenAndUserNotInGroup {

        @Test
        @DisplayName("Then should join group successfully and persist")
        void thenShouldJoinGroupSuccessfullyAndPersist() {
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
        @DisplayName("Then should link user to auth user correctly")
        void thenShouldLinkUserToAuthUserCorrectly() {
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
    }

    @Nested
    @DisplayName("Given invalid join token")
    class WhenInvalidJoinToken {

        @Test
        @DisplayName("Then should throw forbidden exception with invalid token")
        void thenShouldThrowForbiddenExceptionWithInvalidToken() {
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
                    .hasMessageContaining("Invalid join token");

            // And: User should not be modified
            flushAndClear();
            UserEntity unchangedUser = userRepository.findById(joiningUser.getUuid()).orElseThrow();
            assertThat(unchangedUser.getAuthUser()).isNull();
        }
    }

    @Nested
    @DisplayName("Given user already in group")
    class WhenUserAlreadyInGroup {

        @Test
        @DisplayName("Then should throw conflict exception when already joined")
        void thenShouldThrowConflictExceptionWhenAlreadyJoined() {
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
        @DisplayName("Then should detect duplicate by auth user ID")
        void thenShouldDetectDuplicateByAuthUserId() {
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
    }

    @Nested
    @DisplayName("Given user at max group limit for join")
    class WhenUserAtMaxGroupLimitForJoin {

        @Test
        @DisplayName("Then should throw conflict exception when at limit")
        void thenShouldThrowConflictExceptionWhenAtLimit() {
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
    }

    @Nested
    @DisplayName("Given special UID user for join")
    class WhenSpecialUidUserForJoin {

        @Test
        @DisplayName("Then should allow special UID to join beyond limit")
        void thenShouldAllowSpecialUidToJoinBeyondLimit() {
            // Given: Special UID with 12 existing groups
            String specialUid = "v6CGVApOmVM4VWTijmRTg8m01Kj1";
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
    }

    @Nested
    @DisplayName("Given user or auth user does not exist for join")
    class WhenUserOrAuthUserDoesNotExistForJoin {

        @Test
        @DisplayName("Then should throw not found exception when user not found")
        void thenShouldThrowNotFoundExceptionWhenUserNotFound() {
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
        @DisplayName("Then should throw not found exception when auth user not found")
        void thenShouldThrowNotFoundExceptionWhenAuthUserNotFound() {
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
    }

    @Nested
    @DisplayName("Given group does not exist for join")
    class WhenGroupDoesNotExistForJoin {

        @Test
        @DisplayName("Then should throw not found exception when group not found")
        void thenShouldThrowNotFoundExceptionWhenGroupNotFound() {
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
    // leaveGroup Tests (4 nested classes)
    // ========================================

    @Nested
    @DisplayName("Given user is in group")
    class WhenUserIsInGroup {

        @Test
        @DisplayName("Then should leave group successfully and persist")
        void thenShouldLeaveGroupSuccessfullyAndPersist() {
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
        @DisplayName("Then should preserve user entity after leaving")
        void thenShouldPreserveUserEntityAfterLeaving() {
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
        @DisplayName("Then should allow rejoin after leaving")
        void thenShouldAllowRejoinAfterLeaving() {
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
    }

    @Nested
    @DisplayName("Given user not in group for leave")
    class WhenUserNotInGroupForLeave {

        @Test
        @DisplayName("Then should throw not found exception when user not in group")
        void thenShouldThrowNotFoundExceptionWhenUserNotInGroup() {
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
    }

    @Nested
    @DisplayName("Given group does not exist for leave")
    class WhenGroupDoesNotExistForLeave {

        @Test
        @DisplayName("Then should throw not found exception when group not found")
        void thenShouldThrowNotFoundExceptionWhenGroupNotFound() {
            // Given: Non-existent group ID
            UUID nonExistentGroupId = UUID.randomUUID();
            UUID userUuid = UUID.randomUUID();

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.leaveGroup(nonExistentGroupId, userUuid))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");
        }
    }

    @Nested
    @DisplayName("Given user does not exist for leave")
    class WhenUserDoesNotExistForLeave {

        @Test
        @DisplayName("Then should throw not found exception when user not found")
        void thenShouldThrowNotFoundExceptionWhenUserNotFound() {
            // Given: Non-existent user UUID
            UUID nonExistentUserUuid = UUID.randomUUID();

            // When & Then: Should throw not found exception
            assertThatThrownBy(() -> groupService.leaveGroup(testGroupId, nonExistentUserUuid))
                    .hasMessageContaining("User is not in this group");
        }
    }
}
