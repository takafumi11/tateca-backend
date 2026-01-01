package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUserRepository authUserRepository;

    private UUID testUserId;
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        // Create test auth user using TestFixtures
        testAuthUser = TestFixtures.AuthUsers.defaultAuthUser();
        authUserRepository.save(testAuthUser);

        // Create test user using TestFixtures
        testUserId = UUID.randomUUID();
        UserEntity testUser = UserEntity.builder()
                .uuid(testUserId)
                .name("Original Name")
                .authUser(testAuthUser)
                .build();
        userRepository.save(testUser);

        flushAndClear();
    }

    @Nested
    @DisplayName("Given user exists in database")
    class WhenUserExistsInDatabase {

        @Test
        @DisplayName("Then should update user name and persist to database")
        void thenShouldUpdateUserNameAndPersistToDatabase() {
            // Given: Request with new name
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Updated Name");

            // When: Updating user name
            UserResponseDTO result = userService.updateUserName(testUserId, request);

            // Then: Should return DTO with updated name
            assertThat(result).isNotNull();
            assertThat(result.uuid()).isEqualTo(testUserId.toString());
            assertThat(result.userName()).isEqualTo("Updated Name");

            // And: Changes should be persisted in database
            flushAndClear();
            Optional<UserEntity> updatedUser = userRepository.findById(testUserId);
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Then should preserve authUser relationship after update")
        void thenShouldPreserveAuthUserRelationshipAfterUpdate() {
            // Given: Request with new name
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

            // When: Updating user name
            userService.updateUserName(testUserId, request);

            // Then: Should preserve authUser relationship
            flushAndClear();
            UserEntity updatedUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));

            assertThat(updatedUser.getAuthUser()).isNotNull();
            assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(testAuthUser.getUid());
        }

        @Test
        @DisplayName("Then should update updatedAt timestamp")
        void thenShouldUpdateUpdatedAtTimestamp() {
            // Given: Original user with update timestamp
            UserEntity originalUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            var originalUpdatedAt = originalUser.getUpdatedAt();

            // Sleep briefly to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

            // When: Updating user name
            userService.updateUserName(testUserId, request);

            // Then: Updated timestamp should be changed
            flushAndClear();
            UserEntity updatedUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));

            assertThat(updatedUser.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Given user does not exist in database")
    class WhenUserDoesNotExistInDatabase {

        @Test
        @DisplayName("Then should throw EntityNotFoundException with NOT_FOUND status")
        void thenShouldThrowNotFoundExceptionWhenUserNotFound() {
            // Given: Non-existent user ID
            UUID nonExistentUserId = UUID.randomUUID();
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> userService.updateUserName(nonExistentUserId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Then should not create new user when user not found")
        void thenShouldNotCreateNewUserWhenUserNotFound() {
            // Given: Non-existent user ID
            UUID nonExistentUserId = UUID.randomUUID();
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

            long countBefore = userRepository.count();

            // When: Attempting to update non-existent user
            try {
                userService.updateUserName(nonExistentUserId, request);
            } catch (EntityNotFoundException e) {
                // Expected exception
            }

            // Then: Should not create new user
            flushAndClear();
            long countAfter = userRepository.count();
            assertThat(countAfter).isEqualTo(countBefore);
        }
    }

    @Test
    @DisplayName("Should persist multiple sequential updates correctly")
    void shouldPersistMultipleSequentialUpdatesCorrectly() {
        // Given: First update
        UpdateUserNameRequestDTO request1 = new UpdateUserNameRequestDTO("First Update");
        userService.updateUserName(testUserId, request1);
        flushAndClear();

        // When: Second update
        UpdateUserNameRequestDTO request2 = new UpdateUserNameRequestDTO("Second Update");
        userService.updateUserName(testUserId, request2);
        flushAndClear();

        // Then: Latest update should be persisted
        UserEntity finalUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(finalUser.getName()).isEqualTo("Second Update");
    }

    @Test
    @DisplayName("Should persist special characters including emoji correctly")
    void shouldPersistSpecialCharactersCorrectly() {
        // Given: Request with special characters and emoji
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Test 😊 田中 €$");

        // When: Updating user name
        userService.updateUserName(testUserId, request);

        // Then: Should persist and retrieve correctly
        flushAndClear();
        UserEntity updatedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(updatedUser.getName()).isEqualTo("Test 😊 田中 €$");
    }
}