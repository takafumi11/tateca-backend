package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserService Integration Tests — Persistence behavior")
@Transactional
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
        testAuthUser = TestFixtures.AuthUsers.defaultAuthUser();
        authUserRepository.save(testAuthUser);

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
    @DisplayName("@PreUpdate behavior — updated_at timestamp")
    class PreUpdateBehavior {

        @Test
        @DisplayName("Should update updated_at when name changes (via @PreUpdate)")
        void shouldUpdateTimestampOnNameChange() {
            UserEntity originalUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            var originalUpdatedAt = originalUser.getUpdatedAt();

            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var request = new UpdateUserNameRequestDTO("Updated Name");
            userService.updateUserName(testAuthUser.getUid(), testUserId, request);

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updatedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should NOT update updated_at on same-value update (save skipped)")
        void shouldNotUpdateTimestampOnSameValueUpdate() {
            UserEntity originalUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            var originalUpdatedAt = originalUser.getUpdatedAt();

            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var request = new UpdateUserNameRequestDTO("Original Name");
            userService.updateUserName(testAuthUser.getUid(), testUserId, request);

            flushAndClear();
            UserEntity user = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(user.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Database encoding — special characters")
    class DatabaseEncoding {

        @Test
        @DisplayName("Should persist and retrieve multibyte/emoji characters correctly")
        void shouldPersistSpecialCharacters() {
            var request = new UpdateUserNameRequestDTO("Test 😊 田中 €$");
            userService.updateUserName(testAuthUser.getUid(), testUserId, request);

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updatedUser.getName()).isEqualTo("Test 😊 田中 €$");
        }
    }

    @Nested
    @DisplayName("Persistence correctness")
    class PersistenceCorrectness {

        @Test
        @DisplayName("Should persist trimmed name to database")
        void shouldPersistTrimmedName() {
            var request = new UpdateUserNameRequestDTO("  Trimmed Name  ");
            UserResponseDTO result = userService.updateUserName(testAuthUser.getUid(), testUserId, request);

            assertThat(result.userName()).isEqualTo("Trimmed Name");

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updatedUser.getName()).isEqualTo("Trimmed Name");
        }

        @Test
        @DisplayName("Should preserve auth_user relationship after update")
        void shouldPreserveAuthUserRelationship() {
            var request = new UpdateUserNameRequestDTO("Updated Name");
            userService.updateUserName(testAuthUser.getUid(), testUserId, request);

            flushAndClear();
            UserEntity updatedUser = userRepository.findById(testUserId)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updatedUser.getAuthUser()).isNotNull();
            assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(testAuthUser.getUid());
        }
    }
}
