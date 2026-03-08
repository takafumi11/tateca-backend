package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.impl.AuthUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUserServiceImpl — Domain Logic")
class AuthUserServiceUnitTest {

    @Mock private AuthUserRepository repository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AuthUserServiceImpl authUserService;

    private static final String TEST_UID = "test-uid-123";
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        testAuthUser = AuthUserEntity.builder()
                .uid(TEST_UID)
                .name("Test User")
                .email("test@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginTime(Instant.now())
                .totalLoginCount(5)
                .appReviewStatus(AppReviewStatus.PENDING)
                .build();
    }

    // =================================================================
    // getAuthUserInfo
    // =================================================================

    @Nested
    @DisplayName("Given 認証ユーザーが存在する")
    class GivenAuthUserExists_GetInfo {

        @Test
        @DisplayName("Then ログイン回数を1加算して保存する")
        void thenShouldIncrementLoginCountAndSave() {
            when(repository.findById(TEST_UID)).thenReturn(Optional.of(testAuthUser));
            when(repository.save(any(AuthUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthUserResponseDTO result = authUserService.getAuthUserInfo(TEST_UID);

            verify(repository).save(argThat(user ->
                    user.getTotalLoginCount() == 6 && user.getLastLoginTime() != null));
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Given 認証ユーザーが存在しない")
    class GivenAuthUserNotExists_GetInfo {

        @Test
        @DisplayName("Then EntityNotFoundException をスローする")
        void thenShouldThrowEntityNotFoundException() {
            when(repository.findById(TEST_UID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUserService.getAuthUserInfo(TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_USER_NOT_FOUND.getCode());

            verify(repository, never()).save(any());
        }
    }

    // =================================================================
    // createAuthUser
    // =================================================================

    @Nested
    @DisplayName("Given メールアドレスが未登録")
    class GivenEmailNotRegistered {

        @Test
        @DisplayName("Then 正しい初期値でエンティティを作成して保存する")
        void thenShouldCreateEntityWithCorrectDefaults() {
            var request = new CreateAuthUserRequestDTO("new@example.com");
            when(repository.existsByEmail("new@example.com")).thenReturn(false);
            when(repository.save(any(AuthUserEntity.class))).thenAnswer(inv -> {
                AuthUserEntity entity = inv.getArgument(0);
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                return entity;
            });

            authUserService.createAuthUser(TEST_UID, request);

            verify(repository).existsByEmail("new@example.com");
            verify(repository).save(argThat(user ->
                    user.getUid().equals(TEST_UID) &&
                    user.getName().equals("") &&
                    user.getEmail().equals("new@example.com") &&
                    user.getTotalLoginCount() == 1 &&
                    user.getAppReviewStatus() == AppReviewStatus.PENDING &&
                    user.getLastLoginTime() != null));
        }
    }

    @Nested
    @DisplayName("Given メールアドレスが既に登録済み")
    class GivenEmailAlreadyRegistered {

        @Test
        @DisplayName("Then DuplicateResourceException をスローする")
        void thenShouldThrowDuplicateResourceException() {
            var request = new CreateAuthUserRequestDTO("existing@example.com");
            when(repository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authUserService.createAuthUser(TEST_UID, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_USER_EMAIL_DUPLICATE.getCode());

            verify(repository, never()).save(any());
        }
    }

    // =================================================================
    // deleteAuthUser
    // =================================================================

    @Nested
    @DisplayName("Given 認証ユーザーが存在し紐付くアプリ内ユーザーがいる")
    class GivenAuthUserExistsWithLinkedUsers {

        @Test
        @DisplayName("Then 紐付け解除してから削除する")
        void thenShouldUnlinkUsersAndDelete() {
            UserEntity user1 = UserEntity.builder().name("User 1").authUser(testAuthUser).build();
            UserEntity user2 = UserEntity.builder().name("User 2").authUser(testAuthUser).build();
            List<UserEntity> linkedUsers = List.of(user1, user2);

            when(repository.findById(TEST_UID)).thenReturn(Optional.of(testAuthUser));
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(linkedUsers);
            when(userRepository.saveAll(anyList())).thenReturn(linkedUsers);

            authUserService.deleteAuthUser(TEST_UID);

            verify(userRepository).saveAll(argThat(users -> {
                @SuppressWarnings("unchecked")
                List<UserEntity> list = (List<UserEntity>) users;
                return list.stream().allMatch(u -> u.getAuthUser() == null);
            }));
            verify(repository).deleteById(TEST_UID);
        }
    }

    @Nested
    @DisplayName("Given 認証ユーザーが存在しない（削除）")
    class GivenAuthUserNotExists_Delete {

        @Test
        @DisplayName("Then EntityNotFoundException をスローする")
        void thenShouldThrowEntityNotFoundException() {
            when(repository.findById(TEST_UID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUserService.deleteAuthUser(TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_USER_NOT_FOUND.getCode());
        }
    }

    // =================================================================
    // updateAppReview
    // =================================================================

    @Nested
    @DisplayName("Given 認証ユーザーが存在する（レビュー設定更新）")
    class GivenAuthUserExists_UpdateReview {

        @Test
        @DisplayName("Then ステータスとダイアログ表示日時を更新して保存する")
        void thenShouldUpdateStatusAndTimestamp() {
            var request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);
            when(repository.findById(TEST_UID)).thenReturn(Optional.of(testAuthUser));
            when(repository.save(any(AuthUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthUserResponseDTO result = authUserService.updateAppReview(TEST_UID, request);

            verify(repository).save(argThat(user ->
                    user.getAppReviewStatus() == AppReviewStatus.COMPLETED &&
                    user.getLastAppReviewDialogShownAt() != null));
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Given 認証ユーザーが存在しない（レビュー設定更新）")
    class GivenAuthUserNotExists_UpdateReview {

        @Test
        @DisplayName("Then EntityNotFoundException をスローする")
        void thenShouldThrowEntityNotFoundException() {
            var request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);
            when(repository.findById(TEST_UID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUserService.updateAppReview(TEST_UID, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_USER_NOT_FOUND.getCode());

            verify(repository, never()).save(any());
        }
    }
}
