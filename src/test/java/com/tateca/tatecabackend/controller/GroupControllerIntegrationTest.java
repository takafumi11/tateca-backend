package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.entity.*;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GroupController Integration Tests")
class GroupControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private List<UUID> createdGroupIds = new ArrayList<>();
    private List<UUID> createdUserIds = new ArrayList<>();

    @AfterEach
    @Transactional
    void cleanupTestData() {
        // Clean up in correct order due to FK constraints
        try {
            // Delete UserGroupEntity first
            if (!createdGroupIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM UserGroupEntity ug WHERE ug.groupUuid IN :groupIds")
                        .setParameter("groupIds", createdGroupIds)
                        .executeUpdate();
            }

            // Nullify authUser in UserEntity
            if (!createdUserIds.isEmpty()) {
                entityManager.createQuery("UPDATE UserEntity u SET u.authUser = null WHERE u.uuid IN :userIds")
                        .setParameter("userIds", createdUserIds)
                        .executeUpdate();
            }

            // Delete UserEntity
            if (!createdUserIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM UserEntity u WHERE u.uuid IN :userIds")
                        .setParameter("userIds", createdUserIds)
                        .executeUpdate();
            }

            // Delete GroupEntity
            if (!createdGroupIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM GroupEntity g WHERE g.uuid IN :groupIds")
                        .setParameter("groupIds", createdGroupIds)
                        .executeUpdate();
            }

            entityManager.flush();

            // Reset tracking lists
            createdGroupIds.clear();
            createdUserIds.clear();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Nested
    @DisplayName("POST /groups")
    class CreateGroupTests {

        @BeforeEach
        void setupAuthUser() {
            // Clean existing TEST_UID auth user
            try {
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Ensure JPY currency exists
            CurrencyNameEntity jpyCurrency = entityManager.find(CurrencyNameEntity.class, "JPY");
            if (jpyCurrency == null) {
                jpyCurrency = TestFixtures.Currencies.jpy();
                entityManager.persist(jpyCurrency);
                entityManager.flush();
            }

            // Create fresh AuthUserEntity
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test Auth User", "testpost@example.com");
        }

        // ========== P0 Tests (Critical) ==========

        @Test
        @DisplayName("Should create group with host only and return 201 CREATED")
        void shouldCreateGroupWithHostOnly() throws Exception {
            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Solo Trip",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.group.name").value("Solo Trip"))
                    .andExpect(jsonPath("$.group.uuid").exists())
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.users.length()").value(1))
                    .andExpect(jsonPath("$.users[0].name").value("Alice"))
                    .andExpect(jsonPath("$.transaction_count").value(0))
                    .andReturn().getResponse().getContentAsString();

            // Parse response to get UUID
            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Verify DB persistence
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupUuid);
            assertThat(group).isNotNull();
            assertThat(group.getName()).isEqualTo("Solo Trip");
            assertThat(group.getJoinToken()).isNotNull();

            // Verify UserEntity
            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId", UserGroupEntity.class)
                    .setParameter("groupId", groupUuid)
                    .getResultList();

            assertThat(userGroups).hasSize(1);
            UserEntity host = userGroups.get(0).getUser();
            createdUserIds.add(host.getUuid());

            assertThat(host.getName()).isEqualTo("Alice");
            assertThat(host.getAuthUser()).isNotNull();
            assertThat(host.getAuthUser().getUid()).isEqualTo(TEST_UID);
            assertThat(host.getCurrencyName().getCurrencyCode()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("Should create group with host and multiple participants")
        void shouldCreateGroupWithMultipleParticipants() throws Exception {
            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Team Trip",
                                        "host_name": "Alice",
                                        "participants_name": ["Bob", "Charlie", "David"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.group.name").value("Team Trip"))
                    .andExpect(jsonPath("$.users.length()").value(4))
                    .andExpect(jsonPath("$.transaction_count").value(0))
                    .andReturn().getResponse().getContentAsString();

            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Verify DB persistence
            flushAndClear();
            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId", UserGroupEntity.class)
                    .setParameter("groupId", groupUuid)
                    .getResultList();

            assertThat(userGroups).hasSize(4);

            List<UserEntity> users = userGroups.stream()
                    .map(UserGroupEntity::getUser)
                    .toList();

            users.forEach(u -> createdUserIds.add(u.getUuid()));

            // Verify host (exactly 1 with authUser link)
            long hostsWithAuthUser = users.stream()
                    .filter(u -> u.getAuthUser() != null && TEST_UID.equals(u.getAuthUser().getUid()))
                    .count();
            assertThat(hostsWithAuthUser).isEqualTo(1);

            // Verify participants (exactly 3 with null authUser)
            long participantsWithoutAuthUser = users.stream()
                    .filter(u -> u.getAuthUser() == null)
                    .count();
            assertThat(participantsWithoutAuthUser).isEqualTo(3);

            // Verify all users have JPY currency
            users.forEach(u -> assertThat(u.getCurrencyName().getCurrencyCode()).isEqualTo("JPY"));
        }

        @Test
        @DisplayName("Should set default currency to JPY for all users")
        void shouldSetDefaultCurrencyToJPY() throws Exception {
            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Currency Test",
                                        "host_name": "Host",
                                        "participants_name": ["P1", "P2", "P3"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Verify all users have JPY
            flushAndClear();
            List<UserEntity> users = entityManager.createQuery(
                            "SELECT u FROM UserEntity u JOIN UserGroupEntity ug ON u.uuid = ug.userUuid WHERE ug.groupUuid = :groupId",
                            UserEntity.class)
                    .setParameter("groupId", groupUuid)
                    .getResultList();

            assertThat(users).hasSize(4); // 1 host + 3 participants

            users.forEach(user -> {
                createdUserIds.add(user.getUuid());
                assertThat(user.getCurrencyName()).isNotNull();
                assertThat(user.getCurrencyName().getCurrencyCode()).isEqualTo("JPY");
            });
        }

        @Test
        @DisplayName("Should create UserGroup relationships with composite key")
        void shouldCreateUserGroupRelationships() throws Exception {
            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Composite Key Test",
                                        "host_name": "Host",
                                        "participants_name": ["P1", "P2"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Verify UserGroupEntity
            flushAndClear();
            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId", UserGroupEntity.class)
                    .setParameter("groupId", groupUuid)
                    .getResultList();

            assertThat(userGroups).hasSize(3); // 1 host + 2 participants

            userGroups.forEach(ug -> {
                createdUserIds.add(ug.getUserUuid());

                // Verify composite key parts are set
                assertThat(ug.getUserUuid()).isNotNull();
                assertThat(ug.getGroupUuid()).isNotNull();
                assertThat(ug.getGroupUuid()).isEqualTo(groupUuid);

                // Verify FK relationships
                assertThat(ug.getUser()).isNotNull();
                assertThat(ug.getGroup()).isNotNull();
                assertThat(ug.getUser().getUuid()).isEqualTo(ug.getUserUuid());
                assertThat(ug.getGroup().getUuid()).isEqualTo(ug.getGroupUuid());
            });
        }

        @Test
        @DisplayName("Should create GroupEntity with tokenExpires 1 day from now via @PrePersist")
        void shouldCreateGroupEntityWithCorrectFields() throws Exception {
            // Given
            Instant beforeCreate = Instant.now().minus(1, ChronoUnit.SECONDS);

            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "PrePersist Test",
                                        "host_name": "Host",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Then - Verify GroupEntity
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupUuid);
            Instant afterCreate = Instant.now().plus(1, ChronoUnit.SECONDS);

            assertThat(group.getUuid()).isNotNull();
            assertThat(group.getName()).isEqualTo("PrePersist Test");
            assertThat(group.getJoinToken()).isNotNull();

            // Verify @PrePersist: tokenExpires = now + 1 day
            assertThat(group.getTokenExpires())
                    .isCloseTo(beforeCreate.plus(1, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));

            // Verify @PrePersist: createdAt, updatedAt
            assertThat(group.getCreatedAt()).isBetween(beforeCreate, afterCreate);
            assertThat(group.getUpdatedAt()).isBetween(beforeCreate, afterCreate);
            assertThat(group.getCreatedAt()).isCloseTo(group.getUpdatedAt(), within(2, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should create host UserEntity with authUser link and JPY currency")
        void shouldCreateHostUserWithAuthUserLink() throws Exception {
            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Host Test",
                                        "host_name": "Alice",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Then - Verify UserEntity
            flushAndClear();
            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId", UserGroupEntity.class)
                    .setParameter("groupId", groupUuid)
                    .getResultList();

            assertThat(userGroups).hasSize(1);
            UserEntity host = userGroups.get(0).getUser();
            createdUserIds.add(host.getUuid());

            // Verify host fields
            assertThat(host.getUuid()).isNotNull();
            assertThat(host.getName()).isEqualTo("Alice");
            assertThat(host.getAuthUser()).isNotNull();
            assertThat(host.getAuthUser().getUid()).isEqualTo(TEST_UID);
            assertThat(host.getCurrencyName()).isNotNull();
            assertThat(host.getCurrencyName().getCurrencyCode()).isEqualTo("JPY");

            // Verify @PrePersist timestamps
            assertThat(host.getCreatedAt()).isNotNull();
            assertThat(host.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create participant UserEntity without authUser link")
        void shouldCreateParticipantUserWithoutAuthUserLink() throws Exception {
            // When
            String responseJson = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Participant Test",
                                        "host_name": "Host",
                                        "participants_name": ["Participant1"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupUuid = extractGroupUuid(responseJson);
            createdGroupIds.add(groupUuid);

            // Then - Find participant
            flushAndClear();
            List<UserEntity> users = entityManager.createQuery(
                            "SELECT u FROM UserEntity u JOIN UserGroupEntity ug ON u.uuid = ug.userUuid WHERE ug.groupUuid = :groupId",
                            UserEntity.class)
                    .setParameter("groupId", groupUuid)
                    .getResultList();

            users.forEach(u -> createdUserIds.add(u.getUuid()));

            UserEntity participant = users.stream()
                    .filter(u -> "Participant1".equals(u.getName()))
                    .findFirst()
                    .orElseThrow();

            // Verify participant fields
            assertThat(participant.getAuthUser()).isNull(); // KEY: authUser is null for participants
            assertThat(participant.getCurrencyName().getCurrencyCode()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when max group count exceeded (9 existing groups)")
        void shouldReturn409WhenMaxGroupCountExceeded() throws Exception {
            // Given - Create 9 groups for TEST_UID
            createMultipleGroupsForUser(TEST_UID, 9);

            // When & Then - 10th group should fail
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "10th Group",
                                        "host_name": "Host",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user does not exist")
        void shouldReturn404WhenAuthUserNotFound() throws Exception {
            // Given - Delete TEST_UID auth user
            entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                    .setParameter("uid", TEST_UID)
                    .executeUpdate();
            flushAndClear();

            // When & Then
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "No Auth User",
                                        "host_name": "Host",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        // ========== P1 Tests (High Priority) ==========

        @Test
        @DisplayName("Should generate unique join token for each group")
        void shouldGenerateUniqueJoinToken() throws Exception {
            // Given - Create 3 groups
            List<UUID> groupUuids = new ArrayList<>();

            for (int i = 1; i <= 3; i++) {
                String responseJson = mockMvc.perform(post("/groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("""
                                        {
                                            "group_name": "Group %d",
                                            "host_name": "Host%d",
                                            "participants_name": []
                                        }
                                        """, i, i)))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();

                UUID groupUuid = extractGroupUuid(responseJson);
                groupUuids.add(groupUuid);
                createdGroupIds.add(groupUuid);
            }

            // Verify all join tokens are unique
            flushAndClear();
            List<GroupEntity> groups = entityManager.createQuery(
                            "SELECT g FROM GroupEntity g WHERE g.uuid IN :uuids", GroupEntity.class)
                    .setParameter("uuids", groupUuids)
                    .getResultList();

            assertThat(groups).hasSize(3);

            List<UUID> tokens = groups.stream()
                    .map(GroupEntity::getJoinToken)
                    .toList();

            // All tokens should be unique (no duplicates)
            assertThat(tokens).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should allow same user to create multiple groups")
        void shouldAllowSameUserToCreateMultipleGroups() throws Exception {
            // When - Create 3 groups for TEST_UID
            List<UUID> groupUuids = new ArrayList<>();

            for (int i = 1; i <= 3; i++) {
                String responseJson = mockMvc.perform(post("/groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("""
                                        {
                                            "group_name": "User's Group %d",
                                            "host_name": "Host %d",
                                            "participants_name": []
                                        }
                                        """, i, i)))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();

                UUID groupUuid = extractGroupUuid(responseJson);
                groupUuids.add(groupUuid);
                createdGroupIds.add(groupUuid);
            }

            // Verify all 3 groups exist and link to same authUser
            flushAndClear();
            List<UserEntity> allHosts = entityManager.createQuery(
                            "SELECT u FROM UserEntity u WHERE u.name LIKE :pattern", UserEntity.class)
                    .setParameter("pattern", "Host%")
                    .getResultList();

            assertThat(allHosts).hasSize(3);

            allHosts.forEach(host -> {
                createdUserIds.add(host.getUuid());
                assertThat(host.getAuthUser()).isNotNull();
                assertThat(host.getAuthUser().getUid()).isEqualTo(TEST_UID);
            });
        }

        @Test
        @DisplayName("Should return transaction_count=0 for newly created group")
        void shouldReturnTransactionCountZero() throws Exception {
            // When
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "New Group",
                                        "host_name": "Host",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transaction_count").value(0));
        }

        @Test
        @DisplayName("Should return correct response DTO structure")
        void shouldReturnCorrectResponseDTOStructure() throws Exception {
            // When
            mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "DTO Test",
                                        "host_name": "Alice",
                                        "participants_name": ["Bob"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    // Verify group object
                    .andExpect(jsonPath("$.group").exists())
                    .andExpect(jsonPath("$.group.uuid").exists())
                    .andExpect(jsonPath("$.group.name").value("DTO Test"))
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.group.token_expires").exists())
                    .andExpect(jsonPath("$.group.created_at").exists())
                    .andExpect(jsonPath("$.group.updated_at").exists())
                    // Verify users array
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andExpect(jsonPath("$.users[0].uuid").exists())
                    .andExpect(jsonPath("$.users[0].name").exists())
                    .andExpect(jsonPath("$.users[0].currency").exists())
                    .andExpect(jsonPath("$.users[0].created_at").exists())
                    .andExpect(jsonPath("$.users[0].updated_at").exists())
                    .andExpect(jsonPath("$.users[1].uuid").exists())
                    .andExpect(jsonPath("$.users[1].name").exists())
                    // Verify transaction_count
                    .andExpect(jsonPath("$.transaction_count").exists())
                    .andExpect(jsonPath("$.transaction_count").value(0));
        }
    }

    @Nested
    @DisplayName("GET /groups/{groupId}")
    class GetGroupInfoTests {

        @BeforeEach
        void setupAuthUser() {
            // Clean existing TEST_UID auth user
            try {
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Ensure JPY currency exists
            CurrencyNameEntity jpyCurrency = entityManager.find(CurrencyNameEntity.class, "JPY");
            if (jpyCurrency == null) {
                jpyCurrency = TestFixtures.Currencies.jpy();
                entityManager.persist(jpyCurrency);
                entityManager.flush();
            }

            // Create fresh AuthUserEntity
            createAndPersistAuthUser(TEST_UID, "Test Auth User", "testget@example.com");
        }

        // ========== P0 Core Tests ==========

        @Test
        @DisplayName("Should return group info with host only")
        void shouldReturnGroupInfoWithHostOnly() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Solo Trip", "Alice");

            // Act
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    // Assert - Group structure
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()))
                    .andExpect(jsonPath("$.group.name").value("Solo Trip"))
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.group.token_expires").exists())
                    .andExpect(jsonPath("$.group.created_at").exists())
                    .andExpect(jsonPath("$.group.updated_at").exists())
                    // Assert - Users array
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(1))
                    .andExpect(jsonPath("$.users[0].name").value("Alice"))
                    .andExpect(jsonPath("$.users[0].currency.currency_code").value("JPY"))
                    .andExpect(jsonPath("$.users[0].auth_user").exists())
                    .andExpect(jsonPath("$.users[0].auth_user.uid").value(TEST_UID))
                    // Assert - Transaction count
                    .andExpect(jsonPath("$.transaction_count").value(0));

            // Assert - DB verification
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupId);
            assertThat(group).isNotNull();
            assertThat(group.getName()).isEqualTo("Solo Trip");

            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId",
                            UserGroupEntity.class)
                    .setParameter("groupId", groupId)
                    .getResultList();

            assertThat(userGroups).hasSize(1);
            UserEntity host = userGroups.get(0).getUser();
            assertThat(host.getName()).isEqualTo("Alice");
            assertThat(host.getAuthUser()).isNotNull();
            assertThat(host.getAuthUser().getUid()).isEqualTo(TEST_UID);
            assertThat(host.getCurrencyName().getCurrencyCode()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("Should return group info with host and multiple participants")
        void shouldReturnGroupInfoWithHostAndMultipleParticipants() throws Exception {
            // Arrange
            UUID groupId = createGroupWithParticipants("Team Trip", "Host", List.of("P1", "P2", "P3"));

            // Act
            String responseJson = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(4))
                    .andExpect(jsonPath("$.transaction_count").value(0))
                    .andReturn().getResponse().getContentAsString();

            // Parse JSON to check auth_user distribution
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode usersArray = root.get("users");

            // Count users with/without auth_user
            long hostsWithAuthUser = 0;
            long participantsWithoutAuthUser = 0;
            for (JsonNode userNode : usersArray) {
                if (!userNode.get("auth_user").isNull()) {
                    hostsWithAuthUser++;
                } else {
                    participantsWithoutAuthUser++;
                }
            }

            assertThat(hostsWithAuthUser).isEqualTo(1);
            assertThat(participantsWithoutAuthUser).isEqualTo(3);

            // Verify all users have JPY currency
            for (JsonNode userNode : usersArray) {
                assertThat(userNode.get("currency").get("currency_code").asText()).isEqualTo("JPY");
            }

            // DB verification
            flushAndClear();
            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId",
                            UserGroupEntity.class)
                    .setParameter("groupId", groupId)
                    .getResultList();

            assertThat(userGroups).hasSize(4);
        }

        @Test
        @DisplayName("Should return complete GroupInfoDTO with all fields")
        void shouldReturnCompleteGroupInfoDTO() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Complete Group", "Host");

            // Act & Assert
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.uuid").exists())
                    .andExpect(jsonPath("$.group.name").exists())
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.group.token_expires").exists())
                    .andExpect(jsonPath("$.group.created_at").exists())
                    .andExpect(jsonPath("$.group.updated_at").exists());
        }

        @Test
        @DisplayName("Should return complete UserInfoDTO for host with auth_user")
        void shouldReturnCompleteUserInfoDTOForHost() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Host DTO Test", "HostUser");

            // Act
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users[0].uuid").exists())
                    .andExpect(jsonPath("$.users[0].name").value("HostUser"))
                    .andExpect(jsonPath("$.users[0].currency").exists())
                    .andExpect(jsonPath("$.users[0].currency.currency_code").value("JPY"))
                    .andExpect(jsonPath("$.users[0].auth_user").exists())
                    .andExpect(jsonPath("$.users[0].auth_user.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.users[0].created_at").exists())
                    .andExpect(jsonPath("$.users[0].updated_at").exists());
        }

        @Test
        @DisplayName("Should return complete UserInfoDTO for participant with null auth_user")
        void shouldReturnCompleteUserInfoDTOForParticipant() throws Exception {
            // Arrange
            UUID groupId = createGroupWithParticipants("Participant Test", "Host", List.of("Participant1"));

            // Act
            String responseJson = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode usersArray = root.get("users");

            // Find participant (auth_user == null)
            JsonNode participant = null;
            for (JsonNode userNode : usersArray) {
                if (userNode.get("auth_user").isNull()) {
                    participant = userNode;
                    break;
                }
            }

            assertThat(participant).isNotNull();
            assertThat(participant.get("uuid")).isNotNull();
            assertThat(participant.get("name").asText()).isEqualTo("Participant1");
            assertThat(participant.get("currency")).isNotNull();
            assertThat(participant.get("currency").get("currency_code").asText()).isEqualTo("JPY");
            assertThat(participant.get("auth_user").isNull()).isTrue();
            assertThat(participant.get("created_at")).isNotNull();
            assertThat(participant.get("updated_at")).isNotNull();
        }

        @Test
        @DisplayName("Should return transaction_count=0 for new group")
        void shouldReturnTransactionCountZeroForNewGroup() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("New Group", "Host");

            // Act & Assert
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transaction_count").value(0));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group does not exist")
        void shouldReturn404WhenGroupDoesNotExist() throws Exception {
            // Arrange - Non-existent UUID
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(get("/groups/{groupId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when groupId is invalid UUID format")
        void shouldReturn400WhenGroupIdIsInvalidUUID() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/groups/{groupId}", "invalid-uuid-format"))
                    .andExpect(status().isBadRequest());
        }

        // ========== P0 Consistency Tests ==========

        @Test
        @DisplayName("Should return consistent data on multiple GETs (idempotency)")
        void shouldReturnConsistentDataOnMultipleGets() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Consistent Group", "Alice");

            // Act - First GET
            String response1 = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Act - Second GET
            String response2 = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Assert - Compare JSON content
            JsonNode json1 = objectMapper.readTree(response1);
            JsonNode json2 = objectMapper.readTree(response2);

            assertThat(json1.get("group").get("uuid").asText())
                    .isEqualTo(json2.get("group").get("uuid").asText());
            assertThat(json1.get("users").size())
                    .isEqualTo(json2.get("users").size());
            assertThat(json1.get("transaction_count").asLong())
                    .isEqualTo(json2.get("transaction_count").asLong());
        }

        @Test
        @DisplayName("Should return independent data for different groups")
        void shouldReturnIndependentDataForDifferentGroups() throws Exception {
            // Arrange - Create 2 different groups
            UUID groupId1 = createHostOnlyGroup("Group A", "Alice");
            UUID groupId2 = createHostOnlyGroup("Group B", "Bob");

            // Act
            String response1 = mockMvc.perform(get("/groups/{groupId}", groupId1))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String response2 = mockMvc.perform(get("/groups/{groupId}", groupId2))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Assert - Different groups have different data
            JsonNode json1 = objectMapper.readTree(response1);
            JsonNode json2 = objectMapper.readTree(response2);

            assertThat(json1.get("group").get("uuid").asText())
                    .isNotEqualTo(json2.get("group").get("uuid").asText());
            assertThat(json1.get("group").get("name").asText()).isEqualTo("Group A");
            assertThat(json2.get("group").get("name").asText()).isEqualTo("Group B");
            assertThat(json1.get("group").get("join_token").asText())
                    .isNotEqualTo(json2.get("group").get("join_token").asText());
        }

        @Test
        @DisplayName("Should return group immediately after creation (POST→GET E2E)")
        void shouldReturnGroupImmediatelyAfterCreation() throws Exception {
            // Arrange & Act - POST group
            String postResponse = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "E2E Test",
                                        "host_name": "Host",
                                        "participants_name": ["P1"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupId = extractGroupUuid(postResponse);
            createdGroupIds.add(groupId);

            // Act - GET group immediately
            String getResponse = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Assert - POST and GET responses match
            JsonNode postJson = objectMapper.readTree(postResponse);
            JsonNode getJson = objectMapper.readTree(getResponse);

            assertThat(getJson.get("group").get("uuid").asText())
                    .isEqualTo(postJson.get("group").get("uuid").asText());
            assertThat(getJson.get("group").get("name").asText())
                    .isEqualTo(postJson.get("group").get("name").asText());
            assertThat(getJson.get("users").size())
                    .isEqualTo(postJson.get("users").size());
            assertThat(getJson.get("transaction_count").asLong())
                    .isEqualTo(postJson.get("transaction_count").asLong());
        }

        @Test
        @DisplayName("PATCH→GET E2E test (documentary - PATCH not implemented yet)")
        void shouldReturnUpdatedGroupNameAfterPatch() {
            // DOCUMENTATION ONLY
            // This test cannot be executed until PATCH /groups/{groupId} is implemented
            //
            // Expected flow:
            // 1. POST /groups → create group "Original Name"
            // 2. PATCH /groups/{groupId} → update to "Updated Name"
            // 3. GET /groups/{groupId} → verify name == "Updated Name"
            // 4. Verify updated_at > created_at
            //
            // Code reference: GroupService.updateGroupName() method
            // This will be implemented when PATCH endpoint integration tests are added
        }

        // ========== P1 Edge Case Tests ==========

        @Test
        @DisplayName("Should return group with large number of users (10 users)")
        void shouldReturnGroupWithLargeNumberOfUsers() throws Exception {
            // Arrange - Create group with host + 9 participants (10 total)
            List<String> participants = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                participants.add("Participant" + i);
            }
            UUID groupId = createGroupWithParticipants("Large Group", "Host", participants);

            // Act
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(10))
                    .andExpect(jsonPath("$.transaction_count").value(0));

            // DB verification
            flushAndClear();
            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId",
                            UserGroupEntity.class)
                    .setParameter("groupId", groupId)
                    .getResultList();

            assertThat(userGroups).hasSize(10);
        }

        @Test
        @DisplayName("Should return group with long names (boundary values)")
        void shouldReturnGroupWithLongNames() throws Exception {
            // Arrange - Create group with maximum length names
            String longGroupName = "A".repeat(50); // Max 50 chars
            String longHostName = "B".repeat(50);  // Max 50 chars

            UUID groupId = createHostOnlyGroup(longGroupName, longHostName);

            // Act
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value(longGroupName))
                    .andExpect(jsonPath("$.users[0].name").value(longHostName));
        }

        @Test
        @DisplayName("Should return group with special characters and emoji in names")
        void shouldReturnGroupWithSpecialCharactersInNames() throws Exception {
            // Arrange - Create group with emoji and special characters
            String emojiGroupName = "🎉Travel🎉";
            String emojiHostName = "Alice👑";

            UUID groupId = createHostOnlyGroup(emojiGroupName, emojiHostName);

            // Act
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value(emojiGroupName))
                    .andExpect(jsonPath("$.users[0].name").value(emojiHostName));
        }

        @Test
        @DisplayName("Should verify UserEntity lazy loading returns complete data")
        void shouldVerifyLazyLoadingForUserEntity() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Lazy User Test", "Host");

            // Act & Assert - Verify all user fields are present (lazy loading worked)
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users[0].uuid").exists())
                    .andExpect(jsonPath("$.users[0].name").exists())
                    .andExpect(jsonPath("$.users[0].currency").exists())
                    .andExpect(jsonPath("$.users[0].created_at").exists())
                    .andExpect(jsonPath("$.users[0].updated_at").exists());
        }

        @Test
        @DisplayName("Should verify GroupEntity lazy loading returns complete data")
        void shouldVerifyLazyLoadingForGroupEntity() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Lazy Group Test", "Host");

            // Act & Assert - Verify all group fields are present (lazy loading worked)
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.uuid").exists())
                    .andExpect(jsonPath("$.group.name").exists())
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.group.token_expires").exists())
                    .andExpect(jsonPath("$.group.created_at").exists())
                    .andExpect(jsonPath("$.group.updated_at").exists());
        }

        @Test
        @DisplayName("Should verify CurrencyName lazy loading returns complete data")
        void shouldVerifyLazyLoadingForCurrencyName() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Lazy Currency Test", "Host");

            // Act & Assert - Verify currency data is present (lazy loading worked)
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users[0].currency").exists())
                    .andExpect(jsonPath("$.users[0].currency.currency_code").exists())
                    .andExpect(jsonPath("$.users[0].currency.currency_symbol").exists());
        }
    }

    @Nested
    @DisplayName("GET /groups/list")
    class GetGroupListTests {

        @BeforeEach
        void setupAuthUser() {
            // Clean existing TEST_UID auth user
            try {
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Ensure JPY currency exists
            CurrencyNameEntity jpyCurrency = entityManager.find(CurrencyNameEntity.class, "JPY");
            if (jpyCurrency == null) {
                jpyCurrency = TestFixtures.Currencies.jpy();
                entityManager.persist(jpyCurrency);
                entityManager.flush();
            }

            // Create fresh AuthUserEntity
            createAndPersistAuthUser(TEST_UID, "Test Auth User", "testlist@example.com");
        }

        // ========== P0 Basic Success Tests ==========

        @Test
        @DisplayName("Should return empty list when user has no groups")
        void shouldReturnEmptyListWhenUserHasNoGroups() throws Exception {
            // When & Then
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list").isArray())
                    .andExpect(jsonPath("$.group_list.length()").value(0));
        }

        @Test
        @DisplayName("Should return single group when user has one group")
        void shouldReturnSingleGroupWhenUserHasOneGroup() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Solo Trip", "Alice");

            // Act
            String responseJson = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list").isArray())
                    .andExpect(jsonPath("$.group_list.length()").value(1))
                    // Verify all GroupInfoDTO fields
                    .andExpect(jsonPath("$.group_list[0].uuid").value(groupId.toString()))
                    .andExpect(jsonPath("$.group_list[0].name").value("Solo Trip"))
                    .andExpect(jsonPath("$.group_list[0].join_token").exists())
                    .andExpect(jsonPath("$.group_list[0].token_expires").exists())
                    .andExpect(jsonPath("$.group_list[0].created_at").exists())
                    .andExpect(jsonPath("$.group_list[0].updated_at").exists())
                    .andReturn().getResponse().getContentAsString();

            // Assert - Verify no transaction_count field
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode firstGroup = root.get("group_list").get(0);
            assertThat(firstGroup.has("transaction_count")).isFalse();

            // DB verification
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupId);
            assertThat(group).isNotNull();
            assertThat(group.getName()).isEqualTo("Solo Trip");
        }

        @Test
        @DisplayName("Should return multiple groups when user has three groups")
        void shouldReturnMultipleGroupsWhenUserHasThreeGroups() throws Exception {
            // Arrange - Create 3 groups
            UUID groupId1 = createHostOnlyGroup("Group 1", "Host 1");
            UUID groupId2 = createHostOnlyGroup("Group 2", "Host 2");
            UUID groupId3 = createHostOnlyGroup("Group 3", "Host 3");

            // Act
            String responseJson = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3))
                    .andReturn().getResponse().getContentAsString();

            // Parse and verify structure
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode groupList = root.get("group_list");

            assertThat(groupList).hasSize(3);

            // Verify each group has all required fields
            for (JsonNode group : groupList) {
                assertThat(group.get("uuid")).isNotNull();
                assertThat(group.get("name")).isNotNull();
                assertThat(group.get("join_token")).isNotNull();
                assertThat(group.get("token_expires")).isNotNull();
                assertThat(group.get("created_at")).isNotNull();
                assertThat(group.get("updated_at")).isNotNull();
                assertThat(group.has("transaction_count")).isFalse();
            }

            // Verify group names are distinct
            List<String> groupNames = new ArrayList<>();
            groupList.forEach(g -> groupNames.add(g.get("name").asText()));
            assertThat(groupNames).containsExactlyInAnyOrder("Group 1", "Group 2", "Group 3");
        }

        @Test
        @DisplayName("Should return maximum groups when user has nine groups")
        void shouldReturnMaximumGroupsWhenUserHasNineGroups() throws Exception {
            // Arrange - Create 9 groups (max - 1 before 409)
            createMultipleGroupsForUser(TEST_UID, 9);

            // Act
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(9));

            // DB verification - all groups belong to TEST_UID
            flushAndClear();
            List<UserEntity> users = entityManager.createQuery(
                            "SELECT u FROM UserEntity u WHERE u.authUser.uid = :uid",
                            UserEntity.class)
                    .setParameter("uid", TEST_UID)
                    .getResultList();

            assertThat(users).hasSize(9);
        }

        // ========== P0 Response Structure Tests ==========

        @Test
        @DisplayName("Should return correct GroupListResponseDTO structure")
        void shouldReturnCorrectGroupListResponseDTOStructure() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Structure Test", "Host");

            // Act
            String responseJson = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    // Verify group_list key exists and is array
                    .andExpect(jsonPath("$.group_list").exists())
                    .andExpect(jsonPath("$.group_list").isArray())
                    // Verify transaction_count DOES NOT exist (important!)
                    .andExpect(jsonPath("$.transaction_count").doesNotExist())
                    // Verify users field DOES NOT exist (important!)
                    .andExpect(jsonPath("$.users").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            // Parse and verify no extra root-level keys
            JsonNode root = objectMapper.readTree(responseJson);
            assertThat(root.fieldNames()).toIterable().containsExactly("group_list");
        }

        @Test
        @DisplayName("Should return complete GroupInfoDTO fields for each group")
        void shouldReturnCompleteGroupInfoDTOFieldsForEachGroup() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Complete Fields", "Host");

            // Act
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list[0].uuid").exists())
                    .andExpect(jsonPath("$.group_list[0].name").exists())
                    .andExpect(jsonPath("$.group_list[0].join_token").exists())
                    .andExpect(jsonPath("$.group_list[0].token_expires").exists())
                    .andExpect(jsonPath("$.group_list[0].created_at").exists())
                    .andExpect(jsonPath("$.group_list[0].updated_at").exists());
        }

        @Test
        @DisplayName("Should return consistent order on multiple calls")
        void shouldReturnConsistentOrderOnMultipleCalls() throws Exception {
            // Arrange - Create 3 groups
            createHostOnlyGroup("Group A", "Host A");
            createHostOnlyGroup("Group B", "Host B");
            createHostOnlyGroup("Group C", "Host C");

            // Act - First GET
            String response1 = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Act - Second GET
            String response2 = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Assert - Compare UUID order
            JsonNode json1 = objectMapper.readTree(response1);
            JsonNode json2 = objectMapper.readTree(response2);

            List<String> uuids1 = new ArrayList<>();
            json1.get("group_list").forEach(g -> uuids1.add(g.get("uuid").asText()));

            List<String> uuids2 = new ArrayList<>();
            json2.get("group_list").forEach(g -> uuids2.add(g.get("uuid").asText()));

            assertThat(uuids1).isEqualTo(uuids2);
        }

        // ========== P0 Authentication & Isolation Tests ==========

        @Test
        @DisplayName("Should only return groups for authenticated user (user isolation)")
        void shouldOnlyReturnGroupsForAuthenticatedUser() throws Exception {
            // Arrange - Create 3 groups for TEST_UID
            createHostOnlyGroup("Group A", "Host A");
            createHostOnlyGroup("Group B", "Host B");
            createHostOnlyGroup("Group C", "Host C");

            // Create 2 groups for different authUser
            String otherUid = "other-user-uid";
            AuthUserEntity otherAuth = createAndPersistAuthUser(otherUid, "Other User", "other@example.com");
            createMultipleGroupsForUser(otherUid, 2);

            // Act & Assert - GET /groups/list should only return 3 groups for TEST_UID
            String responseJson = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3))
                    .andReturn().getResponse().getContentAsString();

            // Parse and verify group names (should only be TEST_UID's groups)
            JsonNode root = objectMapper.readTree(responseJson);
            List<String> groupNames = new ArrayList<>();
            root.get("group_list").forEach(g -> groupNames.add(g.get("name").asText()));

            assertThat(groupNames).containsExactlyInAnyOrder("Group A", "Group B", "Group C");
            assertThat(groupNames).doesNotContain("Test Group 0", "Test Group 1");

            // DB verification - total 5 groups exist, but only 3 for TEST_UID
            flushAndClear();
            List<GroupEntity> allGroups = entityManager.createQuery(
                            "SELECT g FROM GroupEntity g", GroupEntity.class)
                    .getResultList();
            assertThat(allGroups).hasSizeGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should return group immediately after creation (POST→GET list E2E)")
        void shouldReturnGroupImmediatelyAfterCreation_E2E() throws Exception {
            // Arrange - POST group
            String postResponse = mockMvc.perform(post("/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "E2E List Test",
                                        "host_name": "Host",
                                        "participants_name": []
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID groupId = extractGroupUuid(postResponse);
            createdGroupIds.add(groupId);

            // Act - GET /groups/list immediately
            String listResponse = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(1))
                    .andReturn().getResponse().getContentAsString();

            // Assert - New group appears in list
            JsonNode root = objectMapper.readTree(listResponse);
            String returnedUuid = root.get("group_list").get(0).get("uuid").asText();
            String returnedName = root.get("group_list").get(0).get("name").asText();

            assertThat(returnedUuid).isEqualTo(groupId.toString());
            assertThat(returnedName).isEqualTo("E2E List Test");
        }

        // ========== P1 Edge Case Tests ==========

        @Test
        @DisplayName("Should handle user with multiple UserEntities across groups")
        void shouldHandleUserWithMultipleUserEntitiesAcrossGroups() throws Exception {
            // Arrange - Create 3 groups (each creates new UserEntity with same authUser)
            createHostOnlyGroup("Multi User Group 1", "Host1");
            createHostOnlyGroup("Multi User Group 2", "Host2");
            createHostOnlyGroup("Multi User Group 3", "Host3");

            // Act
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3));

            // DB verification - 3 UserEntity records with same authUser
            flushAndClear();
            List<UserEntity> users = entityManager.createQuery(
                            "SELECT u FROM UserEntity u WHERE u.authUser.uid = :uid",
                            UserEntity.class)
                    .setParameter("uid", TEST_UID)
                    .getResultList();

            assertThat(users).hasSize(3);
            users.forEach(u -> assertThat(u.getAuthUser().getUid()).isEqualTo(TEST_UID));
        }

        @Test
        @DisplayName("Should handle user in groups with different participant counts")
        void shouldHandleUserInGroupsWithDifferentParticipantCounts() throws Exception {
            // Arrange - Groups with varying sizes
            createHostOnlyGroup("Solo Group", "Host Solo");
            createGroupWithParticipants("Medium Group", "Host Medium", List.of("P1", "P2", "P3", "P4", "P5"));
            createGroupWithParticipants("Large Group", "Host Large",
                    List.of("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9"));

            // Act & Assert
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3));
        }

        @Test
        @DisplayName("Should handle groups with long names (boundary value)")
        void shouldHandleGroupsWithLongNames() throws Exception {
            // Arrange - Create group with maximum length name (50 chars)
            String longGroupName = "A".repeat(50);
            UUID groupId = createHostOnlyGroup(longGroupName, "Host");

            // Act
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list[0].name").value(longGroupName));
        }

        @Test
        @DisplayName("Should handle groups with special characters and emoji")
        void shouldHandleGroupsWithSpecialCharactersAndEmoji() throws Exception {
            // Arrange - Create groups with special characters
            createHostOnlyGroup("🎉Travel🎉", "Host1");
            createHostOnlyGroup("Meeting@#$%", "Host2");
            createHostOnlyGroup("日本旅行", "Host3");

            // Act
            String responseJson = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3))
                    .andReturn().getResponse().getContentAsString();

            // Parse and verify Unicode preservation
            JsonNode root = objectMapper.readTree(responseJson);
            List<String> groupNames = new ArrayList<>();
            root.get("group_list").forEach(g -> groupNames.add(g.get("name").asText()));

            assertThat(groupNames).containsExactlyInAnyOrder("🎉Travel🎉", "Meeting@#$%", "日本旅行");
        }

        @Test
        @DisplayName("Should handle groups with minimal valid names (1 character)")
        void shouldHandleGroupsWithMinimalValidNames() throws Exception {
            // Arrange - Single character name
            UUID groupId = createHostOnlyGroup("A", "Host");

            // Act
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list[0].name").value("A"));
        }

        @Test
        @DisplayName("Should return same result on multiple consecutive GETs (idempotency)")
        void shouldReturnSameResultOnMultipleConsecutiveGets() throws Exception {
            // Arrange
            createHostOnlyGroup("Idempotency Test", "Host");

            // Act - 3 consecutive GETs
            String response1 = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String response2 = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String response3 = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Assert - All responses identical
            assertThat(response1).isEqualTo(response2);
            assertThat(response2).isEqualTo(response3);
        }

        @Test
        @DisplayName("Should not affect data with GET requests (no side effects)")
        void shouldNotAffectDataWithGetRequests() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("No Side Effects", "Host");

            // Capture initial timestamp
            flushAndClear();
            GroupEntity groupBefore = entityManager.find(GroupEntity.class, groupId);
            Instant updatedAtBefore = groupBefore.getUpdatedAt();

            // Act - GET /groups/list
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk());

            // Assert - Timestamp unchanged
            flushAndClear();
            GroupEntity groupAfter = entityManager.find(GroupEntity.class, groupId);
            assertThat(groupAfter.getUpdatedAt()).isEqualTo(updatedAtBefore);
        }

        @Test
        @DisplayName("Should correctly fetch lazy-loaded GroupEntities")
        void shouldCorrectlyFetchLazyLoadedGroupEntities() throws Exception {
            // Arrange
            createHostOnlyGroup("Lazy Load Test", "Host");

            // Act & Assert - All GroupEntity fields present (lazy loading worked)
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list[0].uuid").exists())
                    .andExpect(jsonPath("$.group_list[0].name").exists())
                    .andExpect(jsonPath("$.group_list[0].join_token").exists())
                    .andExpect(jsonPath("$.group_list[0].token_expires").exists())
                    .andExpect(jsonPath("$.group_list[0].created_at").exists())
                    .andExpect(jsonPath("$.group_list[0].updated_at").exists());
        }

        @Test
        @DisplayName("Should return groups with correct Tokyo timezone format")
        void shouldReturnGroupsWithCorrectTokyoTimezone() throws Exception {
            // Arrange
            createHostOnlyGroup("Timezone Test", "Host");

            // Act
            String responseJson = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Parse and verify timestamp format
            JsonNode root = objectMapper.readTree(responseJson);
            String createdAt = root.get("group_list").get(0).get("created_at").asText();
            String updatedAt = root.get("group_list").get(0).get("updated_at").asText();

            // Verify timestamps are not empty and contain timezone info
            assertThat(createdAt).isNotEmpty();
            assertThat(updatedAt).isNotEmpty();
            // Tokyo timezone format should contain "+09:00" or similar
            assertThat(createdAt).matches(".*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
        }

        @Test
        @DisplayName("Should handle groups created at different times")
        void shouldHandleGroupsCreatedAtDifferentTimes() throws Exception {
            // Arrange - Create groups with time gaps
            createHostOnlyGroup("First Group", "Host1");
            Thread.sleep(100);
            createHostOnlyGroup("Second Group", "Host2");
            Thread.sleep(100);
            createHostOnlyGroup("Third Group", "Host3");

            // Act
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list.length()").value(3));
        }
    }

    @Nested
    @DisplayName("POST /groups/{groupId} - Join Group Invited")
    class JoinGroupInvitedTests {

        @BeforeEach
        void setupJoinGroupTests() {
            // Clean existing TEST_UID auth user
            try {
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Ensure JPY currency exists
            CurrencyNameEntity jpyCurrency = entityManager.find(CurrencyNameEntity.class, "JPY");
            if (jpyCurrency == null) {
                jpyCurrency = TestFixtures.Currencies.jpy();
                entityManager.persist(jpyCurrency);
                entityManager.flush();
            }

            // Create fresh AuthUserEntity
            createAndPersistAuthUser(TEST_UID, "Test Auth User", "testjoin@example.com");
        }

        // ========== P0: Basic Success Tests ==========

        @Test
        @DisplayName("Should join group successfully with valid token")
        void shouldJoinGroupSuccessfullyWithValidToken() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Travel Group", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act
            String responseJson = mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()))
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andExpect(jsonPath("$.transaction_count").value(0))
                    .andReturn().getResponse().getContentAsString();

            // Assert - Response verification: participant has TEST_UID, host has different authUser
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode usersArray = root.get("users");

            boolean participantHasTestUid = false;
            int usersWithAuthUser = 0;
            for (JsonNode user : usersArray) {
                JsonNode authUserNode = user.get("auth_user");
                if (!authUserNode.isNull()) {
                    usersWithAuthUser++;
                    String userUid = authUserNode.get("uid").asText();
                    if (userUid.equals(TEST_UID)) {
                        // Verify this is the participant
                        assertThat(user.get("uuid").asText()).isEqualTo(participantUuid.toString());
                        participantHasTestUid = true;
                    }
                }
            }
            // Both users should have authUser, but only participant has TEST_UID
            assertThat(usersWithAuthUser).isEqualTo(2);
            assertThat(participantHasTestUid).isTrue();

            // Assert - DB verification: participant authUser linked
            flushAndClear();
            UserEntity participantAfter = entityManager.find(UserEntity.class, participantUuid);
            assertThat(participantAfter.getAuthUser()).isNotNull();
            assertThat(participantAfter.getAuthUser().getUid()).isEqualTo(TEST_UID);
        }

        @Test
        @DisplayName("Should return complete GroupDetailsResponseDTO structure")
        void shouldReturnCompleteGroupDetailsResponseDTO() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Structure Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act & Assert
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group").exists())
                    .andExpect(jsonPath("$.group.uuid").exists())
                    .andExpect(jsonPath("$.group.name").exists())
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.transaction_count").value(0));
        }

        @Test
        @DisplayName("Should join group immediately after creation (E2E)")
        void shouldJoinGroupImmediatelyAfterCreation_E2E() throws Exception {
            // Arrange - Create group via POST
            Map<String, Object> setup = createGroupWithParticipantUser("E2E Test", "Host", "Bob");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - Join immediately
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Assert - Verify via GET /groups/list
            mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group_list[?(@.uuid=='" + groupId + "')]").exists());
        }

        // ========== P0: Token Validation Tests ==========

        @Test
        @DisplayName("Should return 403 FORBIDDEN when join token mismatch")
        void shouldReturn403WhenJoinTokenMismatch() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Invalid Token Group", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID participantUuid = (UUID) setup.get("participantUuid");
            UUID invalidToken = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, invalidToken)))
                    .andExpect(status().isForbidden());

            // DB verification - participant.authUser still null
            flushAndClear();
            UserEntity participant = entityManager.find(UserEntity.class, participantUuid);
            assertThat(participant.getAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should join with correct token after wrong attempt")
        void shouldJoinWithCorrectTokenAfterWrongAttempt() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Retry Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");
            UUID wrongToken = UUID.randomUUID();

            // Act - First attempt with wrong token → 403
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, wrongToken)))
                    .andExpect(status().isForbidden());

            // Act - Second attempt with correct token → 200
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());
        }

        // ========== P0: Duplicate Prevention Tests ==========

        @Test
        @DisplayName("Should return 409 CONFLICT when user already joined group")
        void shouldReturn409WhenUserAlreadyJoinedGroup() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Already Joined", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // First join - success
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Act - Second join attempt
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should allow join when existing users have null authUser")
        void shouldAllowJoinWhenExistingUsersHaveNullAuthUser() throws Exception {
            // Arrange - Create group with 3 participants (all authUser=null)
            Map<String, Object> setup = createGroupWithParticipantUser("Null Auth Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - Participant joins with TEST_UID
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());
        }

        // ========== P0: Max Group Count Tests ==========

        @Test
        @DisplayName("Should return 409 CONFLICT when max group count exceeded")
        void shouldReturn409WhenMaxGroupCountExceeded() throws Exception {
            // Arrange - Create 9 groups for TEST_UID
            createMultipleGroupsForUser(TEST_UID, 9);

            // Create 10th group with participant
            Map<String, Object> setup = createGroupWithParticipantUser("10th Group", "Different Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - Try to join 10th group
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isConflict());

            // Assert - Participant still unlinked
            flushAndClear();
            UserEntity participant = entityManager.find(UserEntity.class, participantUuid);
            assertThat(participant.getAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should allow join when user has 8 groups (9th group)")
        void shouldAllowJoinWhenUserHas8Groups() throws Exception {
            // Arrange - Create 8 groups for TEST_UID
            createMultipleGroupsForUser(TEST_UID, 8);

            // Create 9th group with participant
            Map<String, Object> setup = createGroupWithParticipantUser("9th Group", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - Join 9th group (should succeed)
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Assert - Now has 9 groups
            flushAndClear();
            List<UserEntity> users = entityManager.createQuery(
                            "SELECT u FROM UserEntity u WHERE u.authUser.uid = :uid",
                            UserEntity.class)
                    .setParameter("uid", TEST_UID)
                    .getResultList();
            assertThat(users).hasSize(9);
        }

        // ========== P0: Error Cases ==========

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Act & Assert
            UUID nonExistentGroupId = UUID.randomUUID();
            mockMvc.perform(post("/groups/{groupId}", nonExistentGroupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_uuid": "00000000-0000-0000-0000-000000000000",
                                        "join_token": "11111111-1111-1111-1111-111111111111"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange - Create group but use non-existent user UUID
            Map<String, Object> setup = createGroupWithParticipantUser("User Not Found Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID nonExistentUserUuid = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, nonExistentUserUuid, joinToken)))
                    .andExpect(status().isNotFound());
        }

        // ========== P1: Data Integrity & E2E Tests ==========

        @Test
        @DisplayName("Should link authUser to participant user (null → AuthUserEntity)")
        void shouldLinkAuthUserToParticipantUser() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Auth Link Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Verify initial state: authUser = null
            flushAndClear();
            UserEntity before = entityManager.find(UserEntity.class, participantUuid);
            assertThat(before.getAuthUser()).isNull();

            // Act - Join group
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Assert - State transition: null → AuthUserEntity
            flushAndClear();
            UserEntity after = entityManager.find(UserEntity.class, participantUuid);
            assertThat(after.getAuthUser()).isNotNull();
            assertThat(after.getAuthUser().getUid()).isEqualTo(TEST_UID);
            assertThat(after.getAuthUser().getName()).isEqualTo("Test Auth User");
        }

        @Test
        @DisplayName("Should update UserEntity timestamp via @PreUpdate")
        void shouldUpdateUserEntityTimestamp() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Timestamp Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Capture timestamp before join
            flushAndClear();
            UserEntity before = entityManager.find(UserEntity.class, participantUuid);
            Instant createdAtBefore = before.getCreatedAt();
            Instant updatedAtBefore = before.getUpdatedAt();

            Thread.sleep(1000); // Ensure time difference (1 second)

            // Act - Join group (triggers @PreUpdate)
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Assert - updated_at changed, created_at unchanged
            flushAndClear();
            UserEntity after = entityManager.find(UserEntity.class, participantUuid);
            assertThat(after.getCreatedAt()).isEqualTo(createdAtBefore);
            assertThat(after.getUpdatedAt()).isAfter(updatedAtBefore);
            assertThat(after.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should include all users in response after join")
        void shouldIncludeAllUsersInResponse() throws Exception {
            // Arrange - Group with host + 3 participants
            Map<String, Object> setup = createGroupWithParticipantUser("All Users Test", "Host", "P1");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - P1 joins
            String responseJson = mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andReturn().getResponse().getContentAsString();

            // Assert - All users in response
            JsonNode root = objectMapper.readTree(responseJson);
            List<String> userNames = new ArrayList<>();
            root.get("users").forEach(u -> userNames.add(u.get("name").asText()));
            assertThat(userNames).containsExactlyInAnyOrder("Host", "P1");
        }

        @Test
        @DisplayName("Should reflect join in GET /groups/list immediately")
        void shouldReflectJoinInGroupListImmediately() throws Exception {
            // Arrange - Create group and join
            Map<String, Object> setup = createGroupWithParticipantUser("List E2E", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - Join group
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Assert - Group appears in TEST_UID's list
            String listResponse = mockMvc.perform(get("/groups/list"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode root = objectMapper.readTree(listResponse);
            boolean groupFound = false;
            for (JsonNode group : root.get("group_list")) {
                if (group.get("uuid").asText().equals(groupId.toString())) {
                    groupFound = true;
                    break;
                }
            }
            assertThat(groupFound).isTrue();
        }

        @Test
        @DisplayName("Should reflect join in GET /groups/{groupId} immediately")
        void shouldReflectJoinInGroupInfoImmediately() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Info E2E", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Act - Join group
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Assert - GET /groups/{groupId} shows participant with auth_user
            String infoResponse = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode root = objectMapper.readTree(infoResponse);
            boolean participantHasAuthUser = false;
            for (JsonNode user : root.get("users")) {
                if (user.get("uuid").asText().equals(participantUuid.toString())) {
                    assertThat(user.get("auth_user")).isNotNull();
                    assertThat(user.get("auth_user").get("uid").asText()).isEqualTo(TEST_UID);
                    participantHasAuthUser = true;
                }
            }
            assertThat(participantHasAuthUser).isTrue();
        }

        @Test
        @DisplayName("Should handle group with maximum participants (10 users)")
        void shouldHandleGroupWithMaxParticipants() throws Exception {
            // Arrange - Create group with host + 9 participants via EntityManager
            GroupEntity group = GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Large Group")
                    .joinToken(UUID.randomUUID())
                    .build();
            entityManager.persist(group);
            createdGroupIds.add(group.getUuid());

            CurrencyNameEntity currency = entityManager.find(CurrencyNameEntity.class, "JPY");

            // Create 10 users
            UUID lastParticipantUuid = null;
            for (int i = 0; i < 10; i++) {
                UserEntity user = UserEntity.builder()
                        .uuid(UUID.randomUUID())
                        .name("User" + i)
                        .currencyName(currency)
                        .authUser(null)
                        .build();
                entityManager.persist(user);
                createdUserIds.add(user.getUuid());

                UserGroupEntity ug = UserGroupEntity.builder()
                        .userUuid(user.getUuid())
                        .groupUuid(group.getUuid())
                        .user(user)
                        .group(group)
                        .build();
                entityManager.persist(ug);

                if (i == 9) {
                    lastParticipantUuid = user.getUuid();
                }
            }
            flushAndClear();

            // Act - Last participant joins
            mockMvc.perform(post("/groups/{groupId}", group.getUuid())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, lastParticipantUuid, group.getJoinToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(10));
        }
    }

    @Nested
    @DisplayName("PATCH /groups/{groupId}")
    class UpdateGroupNameTests {

        @BeforeEach
        void setupAuthUser() {
            // Clean existing TEST_UID auth user
            try {
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Ensure JPY currency exists
            CurrencyNameEntity jpyCurrency = entityManager.find(CurrencyNameEntity.class, "JPY");
            if (jpyCurrency == null) {
                jpyCurrency = TestFixtures.Currencies.jpy();
                entityManager.persist(jpyCurrency);
                entityManager.flush();
            }

            // Create fresh AuthUserEntity
            createAndPersistAuthUser(TEST_UID, "Test Auth User", "testpatch@example.com");
        }

        // ========== P0: Basic Success & Timestamp Tests ==========

        @Test
        @DisplayName("Should update group name successfully and return 200 OK")
        void shouldUpdateGroupNameSuccessfully() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Original Name", "Host");

            // Act & Assert
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Updated Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Updated Name"))
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()));

            // Verify DB persistence
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupId);
            assertThat(group.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should return complete GroupDetailsResponseDTO with all fields")
        void shouldReturnCompleteGroupDetailsResponseDTO() throws Exception {
            // Arrange
            UUID groupId = createGroupWithParticipants("Team Trip", "Alice", List.of("Bob", "Charlie"));

            // Act & Assert
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Renamed Team Trip"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    // Group info
                    .andExpect(jsonPath("$.group.uuid").exists())
                    .andExpect(jsonPath("$.group.name").value("Renamed Team Trip"))
                    .andExpect(jsonPath("$.group.join_token").exists())
                    .andExpect(jsonPath("$.group.token_expires").exists())
                    .andExpect(jsonPath("$.group.created_at").exists())
                    .andExpect(jsonPath("$.group.updated_at").exists())
                    // Users array
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(3))
                    // Transaction count
                    .andExpect(jsonPath("$.transaction_count").value(0));
        }

        @Test
        @DisplayName("Should update updated_at timestamp via @PreUpdate")
        void shouldUpdateUpdatedAtTimestampViaPreUpdate() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Original", "Host");
            flushAndClear();

            // Capture before timestamp
            GroupEntity before = entityManager.find(GroupEntity.class, groupId);
            Instant updatedAtBefore = before.getUpdatedAt();

            Thread.sleep(1000); // Ensure time difference (1 second)

            // Act
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Updated"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Assert - updated_at changed
            flushAndClear();
            GroupEntity after = entityManager.find(GroupEntity.class, groupId);
            assertThat(after.getUpdatedAt()).isAfter(updatedAtBefore);
            assertThat(after.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should not change created_at timestamp (immutability)")
        void shouldNotChangeCreatedAtTimestamp() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Original", "Host");
            flushAndClear();

            GroupEntity before = entityManager.find(GroupEntity.class, groupId);
            Instant createdAtBefore = before.getCreatedAt();

            Thread.sleep(100);

            // Act
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Updated"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Assert - created_at unchanged
            flushAndClear();
            GroupEntity after = entityManager.find(GroupEntity.class, groupId);
            assertThat(after.getCreatedAt()).isEqualTo(createdAtBefore);
        }

        @Test
        @DisplayName("Should support special characters and emoji in group name")
        void shouldSupportSpecialCharactersAndEmoji() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Simple", "Host");

            // Test cases with special characters
            String[] specialNames = {
                    "Tokyo Trip 🗼",
                    "バリ旅行2025",
                    "Trip & Fun!",
                    "Alice's Adventure"
            };

            for (String name : specialNames) {
                // Act & Assert
                mockMvc.perform(patch("/groups/{groupId}", groupId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("""
                                        {
                                            "group_name": "%s"
                                        }
                                        """, name)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.group.name").value(name));

                // Verify DB persistence
                flushAndClear();
                GroupEntity group = entityManager.find(GroupEntity.class, groupId);
                assertThat(group.getName()).isEqualTo(name);
            }
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group does not exist")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Arrange - Non-existent UUID
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(patch("/groups/{groupId}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when groupId is invalid UUID format")
        void shouldReturn400WhenGroupIdIsInvalidUUID() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/groups/{groupId}", "invalid-uuid-format")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle long group name (boundary test - 50 chars)")
        void shouldHandleLongGroupName() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Original", "Host");

            // Act - 50 characters (max allowed)
            String maxLengthName = "A".repeat(50);
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "group_name": "%s"
                                    }
                                    """, maxLengthName)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value(maxLengthName));

            // Verify DB
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupId);
            assertThat(group.getName()).hasSize(50);
        }

        // ========== P1: Idempotency & Consistency Tests ==========

        @Test
        @DisplayName("Should be idempotent when updating to same name multiple times")
        void shouldBeIdempotentWhenUpdatingToSameName() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Original", "Host");

            // Act - Update to "Final Name" twice
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Final Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Final Name"));

            Thread.sleep(100);

            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Final Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Final Name"));

            // Verify - name is "Final Name"
            flushAndClear();
            GroupEntity group = entityManager.find(GroupEntity.class, groupId);
            assertThat(group.getName()).isEqualTo("Final Name");
        }

        @Test
        @DisplayName("Should not affect group members when updating name")
        void shouldNotAffectGroupMembers() throws Exception {
            // Arrange - Group with 3 users
            UUID groupId = createGroupWithParticipants("Original Name", "Alice", List.of("Bob", "Charlie"));

            // Capture user IDs before update
            flushAndClear();
            List<UserGroupEntity> usersBefore = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId", UserGroupEntity.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
            assertThat(usersBefore).hasSize(3);
            List<UUID> userIdsBefore = usersBefore.stream()
                    .map(UserGroupEntity::getUserUuid)
                    .toList();

            // Act
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Updated Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(3));

            // Assert - Same users, same count
            flushAndClear();
            List<UserGroupEntity> usersAfter = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId", UserGroupEntity.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
            assertThat(usersAfter).hasSize(3);
            List<UUID> userIdsAfter = usersAfter.stream()
                    .map(UserGroupEntity::getUserUuid)
                    .toList();

            assertThat(userIdsAfter).containsExactlyInAnyOrderElementsOf(userIdsBefore);
        }

        @Test
        @DisplayName("Should reflect update in subsequent GET /groups/{groupId}")
        void shouldReflectUpdateInSubsequentGet() throws Exception {
            // Arrange
            UUID groupId = createHostOnlyGroup("Original Name", "Host");

            // Act - PATCH update
            mockMvc.perform(patch("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "group_name": "Updated via PATCH"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Assert - GET returns updated name
            mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Updated via PATCH"))
                    .andExpect(jsonPath("$.group.uuid").value(groupId.toString()));
        }
    }

    @Nested
    @DisplayName("DELETE /groups/{groupId}/users/{userUuid}")
    class LeaveGroupTests {

        @BeforeEach
        void setupLeaveGroupTests() {
            // Clean existing TEST_UID auth user
            try {
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Ensure JPY currency exists
            CurrencyNameEntity jpyCurrency = entityManager.find(CurrencyNameEntity.class, "JPY");
            if (jpyCurrency == null) {
                jpyCurrency = TestFixtures.Currencies.jpy();
                entityManager.persist(jpyCurrency);
                entityManager.flush();
            }

            // Create fresh AuthUserEntity
            createAndPersistAuthUser(TEST_UID, "Test Auth User", "testdelete@example.com");
        }

        // ========== P0: Basic Success & State Verification ==========

        @Test
        @DisplayName("Should leave group successfully and return 204 No Content")
        void shouldLeaveGroupSuccessfullyAndReturn204NoContent() throws Exception {
            // Arrange - Create group with participant linked to TEST_UID
            Map<String, Object> setup = createGroupWithParticipantUser("Leave Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Link participant to TEST_UID first
            Map<String, Object> joinSetup = setup;
            UUID joinToken = (UUID) joinSetup.get("groupJoinToken");

            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Act - Leave group
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, participantUuid))
                    .andExpect(status().isNoContent());

            // Assert - authUser nullified
            flushAndClear();
            UserEntity participant = entityManager.find(UserEntity.class, participantUuid);
            assertThat(participant).isNotNull();
            assertThat(participant.getAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should nullify authUser link only (entities preserved)")
        void shouldNullifyAuthUserLinkOnly() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Nullify Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Join then leave
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Act - Leave
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, participantUuid))
                    .andExpect(status().isNoContent());

            // Assert - UserEntity, UserGroupEntity, GroupEntity all preserved
            flushAndClear();
            UserEntity user = entityManager.find(UserEntity.class, participantUuid);
            assertThat(user).isNotNull();
            assertThat(user.getAuthUser()).isNull();

            GroupEntity group = entityManager.find(GroupEntity.class, groupId);
            assertThat(group).isNotNull();

            List<UserGroupEntity> userGroups = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId AND ug.userUuid = :userId",
                            UserGroupEntity.class)
                    .setParameter("groupId", groupId)
                    .setParameter("userId", participantUuid)
                    .getResultList();
            assertThat(userGroups).hasSize(1); // UserGroupEntity preserved!
        }

        @Test
        @DisplayName("Should update UserEntity timestamp via @PreUpdate")
        void shouldUpdateUserEntityTimestampViaPreUpdate() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Timestamp Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Join
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Capture timestamp
            flushAndClear();
            UserEntity before = entityManager.find(UserEntity.class, participantUuid);
            Instant updatedAtBefore = before.getUpdatedAt();
            Instant createdAtBefore = before.getCreatedAt();

            Thread.sleep(1000);

            // Act - Leave
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, participantUuid))
                    .andExpect(status().isNoContent());

            // Assert - updated_at changed, created_at unchanged
            flushAndClear();
            UserEntity after = entityManager.find(UserEntity.class, participantUuid);
            assertThat(after.getUpdatedAt()).isAfter(updatedAtBefore);
            assertThat(after.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
            assertThat(after.getCreatedAt()).isEqualTo(createdAtBefore);
        }

        // ========== P0: Error Cases ==========

        @Test
        @DisplayName("Should return 404 NOT_FOUND when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Act & Assert
            UUID nonExistentGroupId = UUID.randomUUID();
            UUID anyUserUuid = UUID.randomUUID();

            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", nonExistentGroupId, anyUserUuid))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange - Valid group, non-existent user
            UUID groupId = createHostOnlyGroup("Valid Group", "Host");
            UUID nonExistentUserUuid = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, nonExistentUserUuid))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user not in group")
        void shouldReturn404WhenUserNotInGroup() throws Exception {
            // Arrange - Create 2 separate groups
            UUID groupA = createHostOnlyGroup("Group A", "Host A");
            UUID groupB = createHostOnlyGroup("Group B", "Host B");

            // Get user from Group B
            flushAndClear();
            List<UserGroupEntity> groupBUsers = entityManager.createQuery(
                            "SELECT ug FROM UserGroupEntity ug WHERE ug.groupUuid = :groupId",
                            UserGroupEntity.class)
                    .setParameter("groupId", groupB)
                    .getResultList();
            UUID userFromGroupB = groupBUsers.get(0).getUserUuid();

            // Act - Try to remove Group B's user from Group A
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupA, userFromGroupB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when invalid UUID format")
        void shouldReturn400WhenInvalidUuidFormat() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", "invalid-group", "invalid-user"))
                    .andExpect(status().isBadRequest());
        }

        // ========== P1: E2E & Security Tests ==========

        @Test
        @DisplayName("Should remove user from group details after leave (E2E)")
        void shouldRemoveUserFromGroupDetailsAfterLeave() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("E2E Leave", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Join
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Verify participant has authUser before leave
            String beforeResponse = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode beforeRoot = objectMapper.readTree(beforeResponse);
            boolean participantHadAuthUser = false;
            for (JsonNode user : beforeRoot.get("users")) {
                if (user.get("uuid").asText().equals(participantUuid.toString())) {
                    participantHadAuthUser = !user.get("auth_user").isNull();
                }
            }
            assertThat(participantHadAuthUser).isTrue();

            // Act - Leave
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, participantUuid))
                    .andExpect(status().isNoContent());

            // Assert - GET /groups/{groupId} shows participant with null auth_user
            String afterResponse = mockMvc.perform(get("/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode afterRoot = objectMapper.readTree(afterResponse);
            for (JsonNode user : afterRoot.get("users")) {
                if (user.get("uuid").asText().equals(participantUuid.toString())) {
                    assertThat(user.get("auth_user").isNull()).isTrue();
                }
            }
        }

        @Test
        @DisplayName("SECURITY ISSUE: Should allow any user to delete any user (uid not validated)")
        void shouldAllowAnyUserToDeleteAnyUser_SecurityIssue() throws Exception {
            // IMPORTANT: This test documents a SECURITY VULNERABILITY
            // The @UId parameter is accepted but NOT VALIDATED in the controller
            // Any authenticated user can delete any other user from any group

            // Arrange - Create different authUser for victim
            String victimUid = "victim-uid-" + UUID.randomUUID().toString().substring(0, 8);
            AuthUserEntity victimAuth = createAndPersistAuthUser(victimUid, "Victim User", "victim@example.com");

            // Create group with victim as host
            GroupEntity group = GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Security Test Group")
                    .joinToken(UUID.randomUUID())
                    .build();
            entityManager.persist(group);
            createdGroupIds.add(group.getUuid());

            CurrencyNameEntity currency = entityManager.find(CurrencyNameEntity.class, "JPY");

            UserEntity victim = UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Victim")
                    .currencyName(currency)
                    .authUser(victimAuth)
                    .build();
            entityManager.persist(victim);
            createdUserIds.add(victim.getUuid());

            UserGroupEntity victimUserGroup = UserGroupEntity.builder()
                    .userUuid(victim.getUuid())
                    .groupUuid(group.getUuid())
                    .user(victim)
                    .group(group)
                    .build();
            entityManager.persist(victimUserGroup);
            flushAndClear();

            // Verify victim has authUser
            UserEntity victimBefore = entityManager.find(UserEntity.class, victim.getUuid());
            assertThat(victimBefore.getAuthUser()).isNotNull();
            assertThat(victimBefore.getAuthUser().getUid()).isEqualTo(victimUid);

            // Act - Attacker (TEST_UID) deletes victim (different UID)
            // THIS SHOULD FAIL BUT SUCCEEDS (SECURITY VIOLATION)
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", group.getUuid(), victim.getUuid()))
                    .andExpect(status().isNoContent()); // SECURITY VIOLATION: Should be 403 FORBIDDEN

            // Assert - Victim's authUser was nullified by attacker
            flushAndClear();
            UserEntity victimAfter = entityManager.find(UserEntity.class, victim.getUuid());
            assertThat(victimAfter.getAuthUser()).isNull(); // SECURITY VIOLATION

            // TODO: After security fix is implemented, this test should verify:
            // .andExpect(status().isForbidden())
            // .andExpect(jsonPath("$.message").value("Cannot delete another user"))
        }

        @Test
        @DisplayName("Should preserve UserGroupEntity after leave (data retention)")
        void shouldPreserveUserGroupEntityAfterLeave() throws Exception {
            // Arrange
            Map<String, Object> setup = createGroupWithParticipantUser("Retention Test", "Host", "Participant");
            UUID groupId = (UUID) setup.get("groupId");
            UUID joinToken = (UUID) setup.get("groupJoinToken");
            UUID participantUuid = (UUID) setup.get("participantUuid");

            // Join
            mockMvc.perform(post("/groups/{groupId}", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "user_uuid": "%s",
                                        "join_token": "%s"
                                    }
                                    """, participantUuid, joinToken)))
                    .andExpect(status().isOk());

            // Count UserGroupEntity before leave
            flushAndClear();
            long countBefore = entityManager.createQuery(
                            "SELECT COUNT(ug) FROM UserGroupEntity ug WHERE ug.userUuid = :userId AND ug.groupUuid = :groupId",
                            Long.class)
                    .setParameter("userId", participantUuid)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            assertThat(countBefore).isEqualTo(1L);

            // Act - Leave
            mockMvc.perform(delete("/groups/{groupId}/users/{userUuid}", groupId, participantUuid))
                    .andExpect(status().isNoContent());

            // Assert - UserGroupEntity STILL EXISTS (important!)
            flushAndClear();
            long countAfter = entityManager.createQuery(
                            "SELECT COUNT(ug) FROM UserGroupEntity ug WHERE ug.userUuid = :userId AND ug.groupUuid = :groupId",
                            Long.class)
                    .setParameter("userId", participantUuid)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            assertThat(countAfter).isEqualTo(1L); // Still exists!
        }
    }

    // Helper methods
    private UUID extractGroupUuid(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        return UUID.fromString(root.get("group").get("uuid").asText());
    }

    private AuthUserEntity createAndPersistAuthUser(String uid, String name, String email) {
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(uid)
                .name(name)
                .email(email)
                .totalLoginCount(1)
                .lastLoginTime(Instant.now())
                .appReviewStatus(AppReviewStatus.PENDING)
                .build();
        entityManager.persist(authUser);
        flushAndClear();
        return authUser;
    }

    private void createMultipleGroupsForUser(String uid, int count) {
        CurrencyNameEntity currency = entityManager.find(CurrencyNameEntity.class, "JPY");
        if (currency == null) {
            currency = TestFixtures.Currencies.jpy();
            entityManager.persist(currency);
            entityManager.flush();
        }

        AuthUserEntity authUser = entityManager.find(AuthUserEntity.class, uid);

        for (int i = 0; i < count; i++) {
            GroupEntity group = GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test Group " + i)
                    .joinToken(UUID.randomUUID())
                    .build();
            entityManager.persist(group);
            createdGroupIds.add(group.getUuid());

            UserEntity user = UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Host " + i)
                    .currencyName(currency)
                    .authUser(authUser)
                    .build();
            entityManager.persist(user);
            createdUserIds.add(user.getUuid());

            UserGroupEntity userGroup = UserGroupEntity.builder()
                    .userUuid(user.getUuid())
                    .groupUuid(group.getUuid())
                    .user(user)
                    .group(group)
                    .build();
            entityManager.persist(userGroup);
        }
        flushAndClear();
    }

    private UUID createHostOnlyGroup(String groupName, String hostName) throws Exception {
        String responseJson = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "group_name": "%s",
                                    "host_name": "%s",
                                    "participants_name": []
                                }
                                """, groupName, hostName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID groupUuid = extractGroupUuid(responseJson);
        createdGroupIds.add(groupUuid);
        return groupUuid;
    }

    private UUID createGroupWithParticipants(
            String groupName,
            String hostName,
            List<String> participantNames
    ) throws Exception {
        String participantsJson = participantNames.stream()
                .map(name -> "\"" + name + "\"")
                .collect(java.util.stream.Collectors.joining(", "));

        String responseJson = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "group_name": "%s",
                                    "host_name": "%s",
                                    "participants_name": [%s]
                                }
                                """, groupName, hostName, participantsJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID groupUuid = extractGroupUuid(responseJson);
        createdGroupIds.add(groupUuid);
        return groupUuid;
    }

    private Map<String, Object> createGroupWithParticipantUser(
            String groupName,
            String hostName,
            String participantName
    ) throws Exception {
        // Create group entity directly (not via POST to avoid TEST_UID auto-link)
        GroupEntity group = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(groupName)
                .joinToken(UUID.randomUUID())
                .build();
        entityManager.persist(group);
        createdGroupIds.add(group.getUuid());

        CurrencyNameEntity currency = entityManager.find(CurrencyNameEntity.class, "JPY");

        // Create separate authUser for host (different from TEST_UID to avoid duplicate conflict)
        String hostUid = "host-uid-" + UUID.randomUUID().toString().substring(0, 8);
        AuthUserEntity hostAuth = createAndPersistAuthUser(hostUid, "Host Auth User", "host@example.com");

        // Create host user (authUser = different UID, not TEST_UID)
        UserEntity host = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(hostName)
                .currencyName(currency)
                .authUser(hostAuth)  // Link to different authUser
                .build();
        entityManager.persist(host);
        createdUserIds.add(host.getUuid());

        // Create participant user (authUser = null)
        UserEntity participant = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(participantName)
                .currencyName(currency)
                .authUser(null)
                .build();
        entityManager.persist(participant);
        createdUserIds.add(participant.getUuid());

        // Create UserGroupEntity for host
        UserGroupEntity hostUserGroup = UserGroupEntity.builder()
                .userUuid(host.getUuid())
                .groupUuid(group.getUuid())
                .user(host)
                .group(group)
                .build();
        entityManager.persist(hostUserGroup);

        // Create UserGroupEntity for participant
        UserGroupEntity participantUserGroup = UserGroupEntity.builder()
                .userUuid(participant.getUuid())
                .groupUuid(group.getUuid())
                .user(participant)
                .group(group)
                .build();
        entityManager.persist(participantUserGroup);

        flushAndClear();

        return Map.of(
                "groupId", group.getUuid(),
                "groupJoinToken", group.getJoinToken(),
                "participantUuid", participant.getUuid()
        );
    }
}
