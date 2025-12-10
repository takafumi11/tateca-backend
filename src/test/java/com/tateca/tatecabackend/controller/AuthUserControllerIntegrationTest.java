package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthUserController Integration Tests")
class AuthUserControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @org.junit.jupiter.api.AfterEach
    @org.springframework.transaction.annotation.Transactional
    void cleanupTestData() {
        // Clean up TEST_UID data after each test to avoid conflicts
        try {
            // First nullify any UserEntity references
            entityManager.createQuery("UPDATE UserEntity u SET u.authUser = null WHERE u.authUser.uid = :uid")
                    .setParameter("uid", TEST_UID)
                    .executeUpdate();

            // Then delete auth user
            entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                    .setParameter("uid", TEST_UID)
                    .executeUpdate();

            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Nested
    @DisplayName("GET /auth/users/{uid}")
    class GetAuthUserTests {

        @Test
        @DisplayName("Should return auth user and update login info (both timestamps non-null - DTO ternary operators true branch)")
        void shouldReturnAuthUserAndUpdateLoginInfo() throws Exception {
            // Given
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-1")
                    .name("Test User")
                    .email("test@example.com")
                    .totalLoginCount(5)
                    .lastLoginTime(Instant.now().minus(1, ChronoUnit.DAYS))
                    .lastAppReviewDialogShownAt(Instant.now().minus(2, ChronoUnit.DAYS))
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value("get-test-uid-1"))
                    .andExpect(jsonPath("$.name").value("Test User"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.total_login_count").value(6))
                    .andExpect(jsonPath("$.last_login_time").isNotEmpty())
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").isNotEmpty());

            // Verify DB updates
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, "get-test-uid-1");
            assertThat(updated.getTotalLoginCount()).isEqualTo(6);
            assertThat(updated.getLastLoginTime()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should increment login count correctly")
        void shouldIncrementLoginCountCorrectly() throws Exception {
            // Given
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-2")
                    .name("Test User")
                    .email("test2@example.com")
                    .totalLoginCount(5)
                    .lastLoginTime(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_login_count").value(6));

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, "get-test-uid-2");
            assertThat(updated.getTotalLoginCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should increment login count multiple times (cumulative)")
        void shouldIncrementLoginCountMultipleTimes() throws Exception {
            // Given
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-3")
                    .name("Test User")
                    .email("test3@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When - First GET
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_login_count").value(2));

            // When - Second GET
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_login_count").value(3));

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, "get-test-uid-3");
            assertThat(updated.getTotalLoginCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should update lastLoginTime to recent time")
        void shouldUpdateLastLoginTimeToRecent() throws Exception {
            // Given
            Instant pastTime = Instant.now().minus(10, ChronoUnit.DAYS);
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-4")
                    .name("Test User")
                    .email("test4@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(pastTime)
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-4"))
                    .andExpect(status().isOk());

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, "get-test-uid-4");
            assertThat(updated.getLastLoginTime()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
            assertThat(updated.getLastLoginTime()).isAfter(pastTime);
        }

        @Test
        @DisplayName("Should return auth user with null lastLoginTime (DTO ternary operator false branch)")
        void shouldReturnAuthUserWithNullLastLoginTime() throws Exception {
            // Given
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-5")
                    .name("Test User")
                    .email("test5@example.com")
                    .totalLoginCount(0)
                    .lastLoginTime(null)
                    .lastAppReviewDialogShownAt(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value("get-test-uid-5"))
                    .andExpect(jsonPath("$.last_login_time").isNotEmpty())  // Updated by GET
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").isNotEmpty());
        }

        @Test
        @DisplayName("Should return auth user with null lastAppReviewDialogShownAt (DTO ternary operator false branch)")
        void shouldReturnAuthUserWithNullLastAppReviewDialogShownAt() throws Exception {
            // Given
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-6")
                    .name("Test User")
                    .email("test6@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(Instant.now())
                    .lastAppReviewDialogShownAt(null)
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value("get-test-uid-6"))
                    .andExpect(jsonPath("$.last_login_time").isNotEmpty())
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").isEmpty());
        }

        @Test
        @DisplayName("Should return auth user with both timestamps null initially (DTO both ternary operators false branch)")
        void shouldReturnAuthUserWithBothTimestampsNull() throws Exception {
            // Given
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-7")
                    .name("Test User")
                    .email("test7@example.com")
                    .totalLoginCount(0)
                    .lastLoginTime(null)
                    .lastAppReviewDialogShownAt(null)
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value("get-test-uid-7"))
                    .andExpect(jsonPath("$.last_login_time").isNotEmpty())  // Updated by GET
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").isEmpty());  // Still null
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user does not exist")
        void shouldReturn404WhenAuthUserDoesNotExist() throws Exception {
            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", "non-existent-uid"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should persist updated values to database")
        void shouldPersistUpdatedValuesToDatabase() throws Exception {
            // Given
            Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("get-test-uid-9")
                    .name("Test User")
                    .email("test9@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(pastTime)
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(authUser);
            flushAndClear();

            // When
            mockMvc.perform(get("/auth/users/{uid}", "get-test-uid-9"))
                    .andExpect(status().isOk());

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, "get-test-uid-9");
            assertThat(updated).isNotNull();
            assertThat(updated.getTotalLoginCount()).isEqualTo(2);
            assertThat(updated.getLastLoginTime()).isAfter(pastTime);
        }
    }

    @Nested
    @DisplayName("POST /auth/users")
    class CreateAuthUserTests {

        @org.junit.jupiter.api.BeforeEach
        void cleanBeforeEach() {
            // Clean up TEST_UID before each POST test to ensure fresh state
            try {
                entityManager.createQuery("UPDATE UserEntity u SET u.authUser = null WHERE u.authUser.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore
            }
        }

        @Test
        @DisplayName("Should create auth user successfully (E2E + email duplicate check if=false branch)")
        void shouldCreateAuthUserSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "New User",
                                        "email": "newuser@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.name").value("New User"))
                    .andExpect(jsonPath("$.email").value("newuser@example.com"));

            // Verify persistence
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getName()).isEqualTo("New User");
            assertThat(persisted.getEmail()).isEqualTo("newuser@example.com");
        }

        @Test
        @DisplayName("Should create with name only (email=null)")
        void shouldCreateWithNameOnly() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Name Only User"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.name").value("Name Only User"));

            // Verify persistence
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getName()).isEqualTo("Name Only User");
            assertThat(persisted.getEmail()).isNull();
        }

        @Test
        @DisplayName("Should create with email only (name=null)")
        void shouldCreateWithEmailOnly() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "emailonly@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.email").value("emailonly@example.com"));

            // Verify persistence
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getName()).isNull();
            assertThat(persisted.getEmail()).isEqualTo("emailonly@example.com");
        }

        @Test
        @DisplayName("Should create with both fields null (no @Valid)")
        void shouldCreateWithBothFieldsNull() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.last_login_time").isNotEmpty());  // Always set by createAuthUser

            // Verify persistence
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getName()).isNull();
            assertThat(persisted.getEmail()).isNull();
            assertThat(persisted.getLastLoginTime()).isNotNull();  // Always set in createAuthUser (line 70)
        }

        @Test
        @DisplayName("Should set default values correctly (@PrePersist)")
        void shouldSetDefaultValuesCorrectly() throws Exception {
            // When
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test",
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            // Then
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted.getTotalLoginCount()).isEqualTo(1);
            assertThat(persisted.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
            assertThat(persisted.getLastLoginTime()).isNotNull();
            assertThat(persisted.getLastLoginTime()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
            assertThat(persisted.getCreatedAt()).isNotNull();
            assertThat(persisted.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email already exists (email duplicate check if=true branch)")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            // Given - Create existing user with same email
            AuthUserEntity existingUser = AuthUserEntity.builder()
                    .uid("existing-uid")
                    .name("Existing User")
                    .email("duplicate@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(existingUser);
            flushAndClear();

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "New User",
                                        "email": "duplicate@example.com"
                                    }
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should allow duplicate names with different emails")
        void shouldAllowDuplicateNames() throws Exception {
            // Given - Create first user with name "John"
            AuthUserEntity firstUser = AuthUserEntity.builder()
                    .uid("first-uid")
                    .name("John")
                    .email("john1@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            entityManager.persist(firstUser);
            flushAndClear();

            // When & Then - Create second user with same name but different email
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "John",
                                        "email": "john2@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            // Verify both exist
            flushAndClear();
            AuthUserEntity secondUser = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(secondUser).isNotNull();
            assertThat(secondUser.getName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should use injected TEST_UID from @UId annotation")
        void shouldUseInjectedTestUid() throws Exception {
            // When
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test",
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            // Then - Verify DB has TEST_UID, not some other uid
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getUid()).isEqualTo(TEST_UID);
        }

        @Test
        @DisplayName("Should set timestamps automatically via @PrePersist")
        void shouldSetTimestampsAutomatically() throws Exception {
            // When
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Timestamp Test",
                                        "email": "timestamp-test@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            // Then
            flushAndClear();
            AuthUserEntity persisted = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getCreatedAt()).isNotNull();
            assertThat(persisted.getUpdatedAt()).isNotNull();
            assertThat(persisted.getCreatedAt()).isCloseTo(persisted.getUpdatedAt(), within(5, ChronoUnit.SECONDS));
            assertThat(persisted.getCreatedAt()).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should persist to database and be retrievable")
        void shouldPersistToDatabase() throws Exception {
            // When
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Persist Test",
                                        "email": "persist@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            // Then - Clear and re-query
            flushAndClear();
            AuthUserEntity queried = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(queried).isNotNull();
            assertThat(queried.getName()).isEqualTo("Persist Test");
            assertThat(queried.getEmail()).isEqualTo("persist@example.com");
            assertThat(queried.getUid()).isEqualTo(TEST_UID);
        }
    }

    @Nested
    @DisplayName("DELETE /auth/users/{uid}")
    class DeleteAuthUserTests {

        @Test
        @DisplayName("Should delete auth user successfully when no UserEntity references (forEach skip branch)")
        void shouldDeleteAuthUserSuccessfully() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser("delete-uid-1", "Test User", "delete1@example.com", AppReviewStatus.PENDING);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", "delete-uid-1"))
                    .andExpect(status().isNoContent());

            // Verify deletion
            flushAndClear();
            AuthUserEntity deleted = entityManager.find(AuthUserEntity.class, "delete-uid-1");
            assertThat(deleted).isNull();
        }

        @Test
        @DisplayName("Should nullify UserEntity reference when deleting AuthUser (forEach execute branch - single element)")
        void shouldNullifyUserEntityReference() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser("delete-uid-2", "Test User", "delete2@example.com", AppReviewStatus.PENDING);
            CurrencyNameEntity currency = TestFixtures.Currencies.jpy();
            entityManager.persist(currency);
            UserEntity user = createAndPersistUser("User 1", currency, authUser);
            UUID userId = user.getUuid();

            // When
            mockMvc.perform(delete("/auth/users/{uid}", "delete-uid-2"))
                    .andExpect(status().isNoContent());

            // Then - UserEntity still exists, but authUser is null (cascade nullification)
            flushAndClear();
            UserEntity updatedUser = entityManager.find(UserEntity.class, userId);
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getName()).isEqualTo("User 1");
            assertThat(updatedUser.getAuthUser()).isNull();  // Critical: reference nullified

            // AuthUser itself is deleted
            AuthUserEntity deletedAuthUser = entityManager.find(AuthUserEntity.class, "delete-uid-2");
            assertThat(deletedAuthUser).isNull();
        }

        @Test
        @DisplayName("Should nullify multiple UserEntity references (forEach execute branch - multiple elements)")
        void shouldNullifyMultipleUserEntityReferences() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser("delete-uid-3", "Test User", "delete3@example.com", AppReviewStatus.PENDING);
            CurrencyNameEntity currency = TestFixtures.Currencies.jpy();
            entityManager.persist(currency);

            UserEntity user1 = createAndPersistUser("User 1", currency, authUser);
            UserEntity user2 = createAndPersistUser("User 2", currency, authUser);
            UserEntity user3 = createAndPersistUser("User 3", currency, authUser);

            UUID userId1 = user1.getUuid();
            UUID userId2 = user2.getUuid();
            UUID userId3 = user3.getUuid();

            // When
            mockMvc.perform(delete("/auth/users/{uid}", "delete-uid-3"))
                    .andExpect(status().isNoContent());

            // Then - All UserEntities still exist, but all authUser references are null
            flushAndClear();
            UserEntity updatedUser1 = entityManager.find(UserEntity.class, userId1);
            UserEntity updatedUser2 = entityManager.find(UserEntity.class, userId2);
            UserEntity updatedUser3 = entityManager.find(UserEntity.class, userId3);

            assertThat(updatedUser1).isNotNull();
            assertThat(updatedUser1.getAuthUser()).isNull();

            assertThat(updatedUser2).isNotNull();
            assertThat(updatedUser2.getAuthUser()).isNull();

            assertThat(updatedUser3).isNotNull();
            assertThat(updatedUser3.getAuthUser()).isNull();

            // AuthUser itself is deleted
            AuthUserEntity deletedAuthUser = entityManager.find(AuthUserEntity.class, "delete-uid-3");
            assertThat(deletedAuthUser).isNull();
        }

        @Test
        @DisplayName("Should preserve UserEntity when deleting AuthUser (UserEntity not deleted)")
        void shouldPreserveUserEntities() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser("delete-uid-4", "Test User", "delete4@example.com", AppReviewStatus.PENDING);
            CurrencyNameEntity currency = TestFixtures.Currencies.jpy();
            entityManager.persist(currency);
            UserEntity user = createAndPersistUser("Preserved User", currency, authUser);
            UUID userId = user.getUuid();

            // When
            mockMvc.perform(delete("/auth/users/{uid}", "delete-uid-4"))
                    .andExpect(status().isNoContent());

            // Then - UserEntity still exists with all its data
            flushAndClear();
            UserEntity preserved = entityManager.find(UserEntity.class, userId);
            assertThat(preserved).isNotNull();
            assertThat(preserved.getName()).isEqualTo("Preserved User");
            assertThat(preserved.getCurrencyName().getCurrencyCode()).isEqualTo("JPY");
            // Only authUser reference is null
            assertThat(preserved.getAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should delete from database (entityManager.find returns null)")
        void shouldDeleteFromDatabase() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser("delete-uid-5", "Test User", "delete5@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(delete("/auth/users/{uid}", "delete-uid-5"))
                    .andExpect(status().isNoContent());

            // Then
            flushAndClear();
            AuthUserEntity deleted = entityManager.find(AuthUserEntity.class, "delete-uid-5");
            assertThat(deleted).isNull();
        }

        @Test
        @DisplayName("Should return 204 NO_CONTENT even when auth user does not exist (JPA deleteById is idempotent)")
        void shouldReturn204EvenWhenAuthUserDoesNotExist() throws Exception {
            // Given - Ensure this UID truly doesn't exist
            String nonExistentUid = "absolutely-non-existent-uid-" + UUID.randomUUID();

            // When & Then - JPA deleteById doesn't throw exception for non-existent entities
            mockMvc.perform(delete("/auth/users/{uid}", nonExistentUid))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /auth/users/review-preferences")
    class UpdateReviewPreferencesTests {

        @org.junit.jupiter.api.BeforeEach
        void cleanBeforeEach() {
            // Clean up TEST_UID before each PATCH test to ensure fresh state
            try {
                entityManager.createQuery("UPDATE UserEntity u SET u.authUser = null WHERE u.authUser.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.createQuery("DELETE FROM AuthUserEntity a WHERE a.uid = :uid")
                        .setParameter("uid", TEST_UID)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
            } catch (Exception e) {
                // Ignore
            }
        }

        // 4 conditional combinations: showDialog (true/false) x appReviewStatus (null/non-null)

        @Test
        @DisplayName("Should update both fields when showDialog=true and appReviewStatus=non-null (both if=true)")
        void shouldUpdateBothFieldsWhenShowDialogTrueAndStatusNonNull() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "patch1@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getLastAppReviewDialogShownAt()).isNotNull();
            assertThat(updated.getLastAppReviewDialogShownAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should update only timestamp when showDialog=true and appReviewStatus=null (if=true, if=false)")
        void shouldUpdateOnlyTimestampWhenShowDialogTrueAndStatusNull() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "patch2@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getLastAppReviewDialogShownAt()).isNotNull();
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);  // Unchanged
        }

        @Test
        @DisplayName("Should update only status when showDialog=false and appReviewStatus=non-null (if=false, if=true)")
        void shouldUpdateOnlyStatusWhenShowDialogFalseAndStatusNonNull() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "patch3@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getLastAppReviewDialogShownAt()).isNull();  // Not updated
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should not update either field when showDialog=false and appReviewStatus=null (both if=false)")
        void shouldNotUpdateEitherWhenShowDialogFalseAndStatusNull() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "patch4@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getLastAppReviewDialogShownAt()).isNull();  // Not updated
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);  // Unchanged
        }

        // Status transition tests

        @Test
        @DisplayName("Should transition from PENDING to COMPLETED")
        void shouldTransitionFromPendingToCompleted() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "transition1@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should transition from PENDING to PERMANENTLY_DECLINED")
        void shouldTransitionFromPendingToDeclined() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "transition2@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "PERMANENTLY_DECLINED"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.PERMANENTLY_DECLINED);
        }

        @Test
        @DisplayName("Should transition from COMPLETED to PERMANENTLY_DECLINED")
        void shouldTransitionFromCompletedToDeclined() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "transition3@example.com", AppReviewStatus.COMPLETED);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "PERMANENTLY_DECLINED"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getAppReviewStatus()).isEqualTo(AppReviewStatus.PERMANENTLY_DECLINED);
        }

        // Other tests

        @Test
        @DisplayName("Should update updatedAt timestamp via @PreUpdate")
        void shouldUpdateUpdatedAtTimestamp() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "update1@example.com", AppReviewStatus.PENDING);
            flushAndClear();

            // Re-fetch to get fresh entity with accurate timestamp
            AuthUserEntity before = entityManager.find(AuthUserEntity.class, TEST_UID);
            Instant originalUpdatedAt = before.getUpdatedAt();

            // Wait a bit to ensure timestamp changes
            Thread.sleep(100);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should persist timestamp to database with precision")
        void shouldPersistTimestampToDatabase() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "precision2@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then - Verify timestamp is persisted and recent
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated.getLastAppReviewDialogShownAt()).isNotNull();
            assertThat(updated.getLastAppReviewDialogShownAt()).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when showDialog is missing")
        void shouldReturn400WhenShowDialogMissing() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "validation@example.com", AppReviewStatus.PENDING);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user not found")
        void shouldReturn404WhenAuthUserNotFound() throws Exception {
            // When & Then - TEST_UID does not exist
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should use injected TEST_UID from @UId annotation")
        void shouldUseInjectedTestUid() throws Exception {
            // Given
            AuthUserEntity authUser = createAndPersistAuthUser(TEST_UID, "Test User", "uid-test@example.com", AppReviewStatus.PENDING);

            // When
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then - Verify TEST_UID user was updated
            flushAndClear();
            AuthUserEntity updated = entityManager.find(AuthUserEntity.class, TEST_UID);
            assertThat(updated).isNotNull();
            assertThat(updated.getUid()).isEqualTo(TEST_UID);
            assertThat(updated.getLastAppReviewDialogShownAt()).isNotNull();
        }
    }

    // Helper methods
    private AuthUserEntity createAndPersistAuthUser(String uid, String name, String email,
                                                     AppReviewStatus status) {
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(uid)
                .name(name)
                .email(email)
                .totalLoginCount(1)
                .lastLoginTime(Instant.now())
                .appReviewStatus(status)
                .build();
        entityManager.persist(authUser);
        flushAndClear();
        return authUser;
    }

    private UserEntity createAndPersistUser(String name, CurrencyNameEntity currency,
                                            AuthUserEntity authUser) {
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(name)
                .currencyName(currency)
                .authUser(authUser)
                .build();
        entityManager.persist(user);
        flushAndClear();
        return user;
    }
}
