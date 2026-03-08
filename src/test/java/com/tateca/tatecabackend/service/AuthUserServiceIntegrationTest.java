package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthUserService Integration Tests — Infrastructure behavior")
@Transactional
class AuthUserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AuthUserService authUserService;
    @Autowired private AuthUserRepository authUserRepository;
    @Autowired private UserRepository userRepository;

    private static final String TEST_UID = "test-uid-" + System.currentTimeMillis();
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
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

    @Nested
    @DisplayName("@PrePersist / @PreUpdate — タイムスタンプ自動設定")
    class PrePersistPreUpdateBehavior {

        @Test
        @DisplayName("Should set createdAt and updatedAt on @PrePersist when creating auth user")
        void shouldSetTimestampsOnCreate() {
            String newUid = "new-uid-" + System.currentTimeMillis();
            var request = new CreateAuthUserRequestDTO("new" + System.currentTimeMillis() + "@example.com");

            authUserService.createAuthUser(newUid, request);
            flushAndClear();

            AuthUserEntity created = authUserRepository.findById(newUid)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(created.getCreatedAt()).isNotNull();
            assertThat(created.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should update updatedAt via @PreUpdate on app review update")
        void shouldUpdateTimestampOnUpdate() {
            AuthUserEntity before = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            Instant originalUpdatedAt = before.getUpdatedAt();

            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);
            authUserService.updateAppReview(TEST_UID, request);
            flushAndClear();

            AuthUserEntity after = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(after.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("ログイン回数 — 累積永続化")
    class LoginCountPersistence {

        @Test
        @DisplayName("Should persist incremented login count across multiple calls")
        void shouldPersistIncrementedLoginCount() {
            int initial = testAuthUser.getTotalLoginCount();

            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();
            authUserService.getAuthUserInfo(TEST_UID);
            flushAndClear();

            AuthUserEntity updated = authUserRepository.findById(TEST_UID)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(updated.getTotalLoginCount()).isEqualTo(initial + 3);
        }
    }

    @Nested
    @DisplayName("削除 — 紐付け解除とアプリ内ユーザー保持")
    class DeleteUnlinkBehavior {

        @Test
        @DisplayName("Should nullify authUser reference and preserve user entities in DB")
        void shouldNullifyAndPreserveUsers() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            userRepository.save(UserEntity.builder()
                    .uuid(userId1).name("User 1").authUser(testAuthUser).build());
            userRepository.save(UserEntity.builder()
                    .uuid(userId2).name("User 2").authUser(testAuthUser).build());
            flushAndClear();

            authUserService.deleteAuthUser(TEST_UID);
            flushAndClear();

            // Auth user should be deleted
            assertThat(authUserRepository.findById(TEST_UID)).isEmpty();

            // App users should still exist with null authUser
            UserEntity u1 = userRepository.findById(userId1)
                    .orElseThrow(() -> new AssertionError("User 1 should still exist"));
            UserEntity u2 = userRepository.findById(userId2)
                    .orElseThrow(() -> new AssertionError("User 2 should still exist"));
            assertThat(u1.getAuthUser()).isNull();
            assertThat(u2.getAuthUser()).isNull();
            assertThat(u1.getName()).isEqualTo("User 1");
        }
    }

    @Nested
    @DisplayName("メール一意性 — DB 制約")
    class EmailUniquenessConstraint {

        @Test
        @DisplayName("Should enforce email uniqueness at repository level")
        void shouldEnforceEmailUniqueness() {
            String existingEmail = testAuthUser.getEmail();

            boolean exists = authUserRepository.existsByEmail(existingEmail);
            assertThat(exists).isTrue();

            boolean notExists = authUserRepository.existsByEmail("nonexistent@example.com");
            assertThat(notExists).isFalse();
        }
    }

    @Nested
    @DisplayName("AppReviewStatus — DB enum 永続化")
    class AppReviewStatusPersistence {

        @Test
        @DisplayName("Should persist all AppReviewStatus enum values correctly")
        void shouldPersistAllStatusValues() {
            for (AppReviewStatus status : AppReviewStatus.values()) {
                var request = new UpdateAppReviewRequestDTO(status);
                authUserService.updateAppReview(TEST_UID, request);
                flushAndClear();

                AuthUserEntity updated = authUserRepository.findById(TEST_UID)
                        .orElseThrow(() -> new AssertionError("User should exist"));
                assertThat(updated.getAppReviewStatus()).isEqualTo(status);
            }
        }
    }

    @Nested
    @DisplayName("作成 — 初期値の永続化")
    class CreateDefaultsPersistence {

        @Test
        @DisplayName("Should persist default values (empty name, loginCount=1, PENDING status)")
        void shouldPersistDefaults() {
            String newUid = "defaults-uid-" + System.currentTimeMillis();
            String email = "defaults" + System.currentTimeMillis() + "@example.com";
            var request = new CreateAuthUserRequestDTO(email);

            authUserService.createAuthUser(newUid, request);
            flushAndClear();

            AuthUserEntity created = authUserRepository.findById(newUid)
                    .orElseThrow(() -> new AssertionError("User should exist"));
            assertThat(created.getName()).isEqualTo("");
            assertThat(created.getEmail()).isEqualTo(email);
            assertThat(created.getTotalLoginCount()).isEqualTo(1);
            assertThat(created.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
            assertThat(created.getLastLoginTime()).isNotNull();
            assertThat(created.getLastAppReviewDialogShownAt()).isNull();
        }
    }
}
