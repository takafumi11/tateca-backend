package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthUserService Integration Tests")
class AuthUserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthUserService authUserService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_UID = "test-uid-" + System.currentTimeMillis();
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        // Create test auth user
        testAuthUser = AuthUserEntity.builder()
                .uid(TEST_UID)
                .name("Test User")
                .email("test" + System.currentTimeMillis() + "@example.com")
                .lastLoginTime(Instant.now())
                .totalLoginCount(5)
                .appReviewStatus(AppReviewStatus.PENDING)
                .build();
        authUserRepository.save(testAuthUser);

        flushAndClear();
    }

    // ========================================
    // getAuthUserInfo Tests
    // ========================================

    @Nested
    @DisplayName("Given auth user exists in database")
    class WhenAuthUserExistsInDatabase {

        @Test
        @DisplayName("Then should update login count and timestamp and persist to database")
        void thenShouldUpdateLoginCountAndTimestampAndPersistToDatabase() {
            // Given: Existing auth user with login count 5
            int originalLoginCount = testAuthUser.getTotalLoginCount();

            // When: Getting user info
            AuthUserResponseDTO result = authUserService.getAuthUserInfo(TEST_UID);

            // Then: Should return DTO with incremented login count
            assertThat(result).isNotNull();
            assertThat(result.uid()).isEqualTo(TEST_UID);
            assertThat(result.totalLoginCount()).isEqualTo(originalLoginCount + 1);
            assertThat(result.lastLoginTime()).isNotNull();

            // And: Changes should be persisted in database
            flushAndClear();
            Optional<AuthUserEntity> updatedUser = authUserRepository.findById(TEST_UID);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getTotalLoginCount()).isEqualTo(originalLoginCount + 1);
            assertThat(updatedUser.get().getLastLoginTime()).isNotNull();
        }

        @Test
        @DisplayName("Then should increment login count on multiple calls")
        void thenShouldIncrementLoginCountOnMultipleCalls() {
            // Given: Initial login count
            int initialCount = testAuthUser.getTotalLoginCount();

            // When: Getting user info multiple times
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();

            // Then: Login count should be incremented 3 times
            AuthUserEntity updatedUser = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updatedUser.getTotalLoginCount()).isEqualTo(initialCount + 3);
        }

        @Test
        @DisplayName("Then should update lastLoginTime on each call")
        void thenShouldUpdateLastLoginTimeOnEachCall() {
            // Given: Get user info first time
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();

            AuthUserEntity firstUpdate = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            Instant firstLoginTime = firstUpdate.getLastLoginTime();

            // Sleep briefly to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When: Getting user info second time
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();

            // Then: Last login time should be updated
            AuthUserEntity secondUpdate = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(secondUpdate.getLastLoginTime()).isAfterOrEqualTo(firstLoginTime);
        }
    }

    @Nested
    @DisplayName("Given auth user does not exist in database")
    class WhenAuthUserDoesNotExistInDatabase {

        @Test
        @DisplayName("Then should throw ResponseStatusException with NOT_FOUND status")
        void thenShouldThrowNotFoundExceptionWhenUserNotFound() {
            // Given: Non-existent user UID
            String nonExistentUid = "non-existent-uid";

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> authUserService.getAuthUserInfo(nonExistentUid))
                    .isInstanceOf(com.tateca.tatecabackend.exception.domain.EntityNotFoundException.class)
                    .hasMessageContaining("Auth user not found");
        }
    }

    // ========================================
    // createAuthUser Tests
    // ========================================

    @Nested
    @DisplayName("Given valid user data")
    class WhenCreatingAuthUserWithValidData {

        @Test
        @DisplayName("Then should create and persist user to database")
        void thenShouldCreateAndPersistUserToDatabase() {
            // Given: Valid request for new user
            String newUid = "new-uid-" + System.currentTimeMillis();
            String newEmail = "newuser" + System.currentTimeMillis() + "@example.com";
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(newEmail);

            // When: Creating user
            AuthUserResponseDTO result = authUserService.createAuthUser(newUid, request);

            // Then: Should return DTO with correct data
            assertThat(result).isNotNull();
            assertThat(result.uid()).isEqualTo(newUid);
            assertThat(result.name()).isEqualTo("");
            assertThat(result.email()).isEqualTo(newEmail);
            assertThat(result.totalLoginCount()).isEqualTo(1);
            assertThat(result.lastLoginTime()).isNotNull();
            assertThat(result.appReviewStatus()).isEqualTo(AppReviewStatus.PENDING);

            // And: Should be persisted in database
            flushAndClear();
            Optional<AuthUserEntity> createdUser = authUserRepository.findById(newUid);
            assertThat(createdUser).isPresent();
            assertThat(createdUser.get().getName()).isEqualTo("");
            assertThat(createdUser.get().getEmail()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("Then should set default values correctly")
        void thenShouldSetDefaultValuesCorrectly() {
            // Given: Valid request
            String newUid = "default-uid-" + System.currentTimeMillis();
            String newEmail = "default" + System.currentTimeMillis() + "@example.com";
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(newEmail);

            // When: Creating user
            authUserService.createAuthUser(newUid, request);

            // Then: Should set correct default values
            flushAndClear();
            AuthUserEntity createdUser = authUserRepository.findById(newUid)
                    .orElseThrow(() -> new AssertionError("User should exist"));

            assertThat(createdUser.getTotalLoginCount()).isEqualTo(1);
            assertThat(createdUser.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
            assertThat(createdUser.getLastLoginTime()).isNotNull();
            assertThat(createdUser.getLastAppReviewDialogShownAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Given email already exists")
    class WhenEmailAlreadyExists {

        @Test
        @DisplayName("Then should throw DuplicateResourceException")
        void thenShouldThrowConflictExceptionWhenEmailExists() {
            // Given: Existing user with email
            String existingEmail = testAuthUser.getEmail();
            String newUid = "conflict-uid-" + System.currentTimeMillis();
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(existingEmail);

            // When & Then: Should throw DuplicateResourceException
            assertThatThrownBy(() -> authUserService.createAuthUser(newUid, request))
                    .isInstanceOf(com.tateca.tatecabackend.exception.domain.DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");

            // And: Should not create new user
            flushAndClear();
            Optional<AuthUserEntity> conflictUser = authUserRepository.findById(newUid);
            assertThat(conflictUser).isEmpty();
        }
    }

    // ========================================
    // deleteAuthUser Tests
    // ========================================

    @Nested
    @DisplayName("Given auth user exists without associated users")
    class WhenDeletingAuthUserWithoutAssociatedUsers {

        @Test
        @DisplayName("Then should delete auth user from database")
        void thenShouldDeleteAuthUserFromDatabase() {
            // Given: Existing auth user
            long countBefore = authUserRepository.count();

            // When: Deleting auth user
            authUserService.deleteAuthUser(TEST_UID);

            // Then: User should be deleted from database
            flushAndClear();
            Optional<AuthUserEntity> deletedUser = authUserRepository.findById(TEST_UID);
            assertThat(deletedUser).isEmpty();
            assertThat(authUserRepository.count()).isEqualTo(countBefore - 1);
        }
    }

    @Nested
    @DisplayName("Given auth user exists with associated users")
    class WhenDeletingAuthUserWithAssociatedUsers {

        @Test
        @DisplayName("Then should nullify authUser reference in associated users before deleting")
        void thenShouldNullifyAuthUserReferenceBeforeDeleting() {
            // Given: Auth user with associated users
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            UserEntity user1 = UserEntity.builder()
                    .uuid(userId1)
                    .name("Associated User 1")
                    .authUser(testAuthUser)
                    .build();

            UserEntity user2 = UserEntity.builder()
                    .uuid(userId2)
                    .name("Associated User 2")
                    .authUser(testAuthUser)
                    .build();

            userRepository.save(user1);
            userRepository.save(user2);
            flushAndClear();

            // When: Deleting auth user
            authUserService.deleteAuthUser(TEST_UID);

            // Then: Associated users should have nullified authUser reference
            flushAndClear();
            UserEntity updatedUser1 = userRepository.findById(userId1)
                    .orElseThrow(() -> new AssertionError("User 1 should still exist"));
            UserEntity updatedUser2 = userRepository.findById(userId2)
                    .orElseThrow(() -> new AssertionError("User 2 should still exist"));

            assertThat(updatedUser1.getAuthUser()).isNull();
            assertThat(updatedUser2.getAuthUser()).isNull();

            // And: Auth user should be deleted
            Optional<AuthUserEntity> deletedAuthUser = authUserRepository.findById(TEST_UID);
            assertThat(deletedAuthUser).isEmpty();
        }

        @Test
        @DisplayName("Then should preserve associated user entities after auth user deletion")
        void thenShouldPreserveAssociatedUserEntities() {
            // Given: Auth user with associated users
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder()
                    .uuid(userId)
                    .name("Preserved User")
                    .authUser(testAuthUser)
                    .build();
            userRepository.save(user);
            flushAndClear();

            long userCountBefore = userRepository.count();

            // When: Deleting auth user
            authUserService.deleteAuthUser(TEST_UID);

            // Then: User entities should still exist
            flushAndClear();
            long userCountAfter = userRepository.count();
            assertThat(userCountAfter).isEqualTo(userCountBefore);

            UserEntity preservedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new AssertionError("User should still exist"));
            assertThat(preservedUser.getName()).isEqualTo("Preserved User");
        }
    }

    @Nested
    @DisplayName("Given auth user does not exist")
    class WhenDeletingNonExistentAuthUser {

        @Test
        @DisplayName("Then should throw ResponseStatusException with NOT_FOUND status")
        void thenShouldThrowNotFoundExceptionWhenUserNotFound() {
            // Given: Non-existent user UID
            String nonExistentUid = "non-existent-uid";

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> authUserService.deleteAuthUser(nonExistentUid))
                    .isInstanceOf(com.tateca.tatecabackend.exception.domain.EntityNotFoundException.class)
                    .hasMessageContaining("Auth user not found");
        }
    }

    // ========================================
    // updateAppReview Tests
    // ========================================

    @Nested
    @DisplayName("Given auth user exists")
    class WhenUpdatingAppReviewPreferences {

        @Test
        @DisplayName("Then should update app review status and timestamp and persist to database")
        void thenShouldUpdateAppReviewStatusAndTimestampAndPersistToDatabase() {
            // Given: Existing auth user with PENDING status
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            // When: Updating app review
            AuthUserResponseDTO result = authUserService.updateAppReview(TEST_UID, request);

            // Then: Should return DTO with updated status
            assertThat(result).isNotNull();
            assertThat(result.uid()).isEqualTo(TEST_UID);
            assertThat(result.appReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
            assertThat(result.lastAppReviewDialogShownAt()).isNotNull();

            // And: Changes should be persisted in database
            flushAndClear();
            AuthUserEntity updatedUser = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updatedUser.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
            assertThat(updatedUser.getLastAppReviewDialogShownAt()).isNotNull();
        }

        @Test
        @DisplayName("Then should update lastAppReviewDialogShownAt on each call")
        void thenShouldUpdateLastAppReviewDialogShownAtOnEachCall() {
            // Given: First update
            UpdateAppReviewRequestDTO request1 = new UpdateAppReviewRequestDTO(AppReviewStatus.PENDING);
            authUserService.updateAppReview(TEST_UID, request1);
            flushAndClear();

            AuthUserEntity firstUpdate = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            Instant firstTimestamp = firstUpdate.getLastAppReviewDialogShownAt();

            // Sleep briefly to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When: Second update
            UpdateAppReviewRequestDTO request2 = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);
            authUserService.updateAppReview(TEST_UID, request2);
            flushAndClear();

            // Then: Timestamp should be updated
            AuthUserEntity secondUpdate = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(secondUpdate.getLastAppReviewDialogShownAt()).isAfterOrEqualTo(firstTimestamp);
        }

        @Test
        @DisplayName("Then should support all app review status values")
        void thenShouldSupportAllAppReviewStatusValues() {
            // Given: Requests for all status values
            UpdateAppReviewRequestDTO pendingRequest = new UpdateAppReviewRequestDTO(AppReviewStatus.PENDING);
            UpdateAppReviewRequestDTO completedRequest = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);
            UpdateAppReviewRequestDTO declinedRequest = new UpdateAppReviewRequestDTO(AppReviewStatus.PERMANENTLY_DECLINED);

            // When & Then: Should update to PENDING
            authUserService.updateAppReview(TEST_UID, pendingRequest);
            flushAndClear();
            assertThat(authUserRepository.findById(TEST_UID).get().getAppReviewStatus())
                    .isEqualTo(AppReviewStatus.PENDING);

            // When & Then: Should update to COMPLETED
            authUserService.updateAppReview(TEST_UID, completedRequest);
            flushAndClear();
            assertThat(authUserRepository.findById(TEST_UID).get().getAppReviewStatus())
                    .isEqualTo(AppReviewStatus.COMPLETED);

            // When & Then: Should update to PERMANENTLY_DECLINED
            authUserService.updateAppReview(TEST_UID, declinedRequest);
            flushAndClear();
            assertThat(authUserRepository.findById(TEST_UID).get().getAppReviewStatus())
                    .isEqualTo(AppReviewStatus.PERMANENTLY_DECLINED);
        }
    }

    @Nested
    @DisplayName("Given auth user does not exist")
    class WhenUpdatingAppReviewForNonExistentUser {

        @Test
        @DisplayName("Then should throw ResponseStatusException with NOT_FOUND status")
        void thenShouldThrowNotFoundExceptionWhenUserNotFound() {
            // Given: Non-existent user UID
            String nonExistentUid = "non-existent-uid";
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> authUserService.updateAppReview(nonExistentUid, request))
                    .isInstanceOf(com.tateca.tatecabackend.exception.domain.EntityNotFoundException.class)
                    .hasMessageContaining("Auth user not found");
        }
    }
}
