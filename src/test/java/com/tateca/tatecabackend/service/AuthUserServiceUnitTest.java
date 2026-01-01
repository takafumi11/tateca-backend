package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUserService Unit Tests")
class AuthUserServiceUnitTest {

    @Mock
    private AuthUserRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthUserServiceImpl authUserService;

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

    // ========================================
    // getAuthUserInfo Tests
    // ========================================

    @Nested
    @DisplayName("getAuthUserInfo")
    class GetAuthUserInfo {

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not found")
        void shouldThrowEntityNotFoundExceptionWhenUserNotFound() {
            // Given: Repository returns empty
            when(repository.findById(TEST_UID))
                    .thenReturn(Optional.empty());

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> authUserService.getAuthUserInfo(TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Auth user not found");

            // And: Save should not be called
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should update login count and timestamp")
        void shouldUpdateLoginCountAndTimestamp() {
            // Given: Existing user
            when(repository.findById(TEST_UID)).thenReturn(Optional.of(testAuthUser));
            when(repository.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Getting user info
            AuthUserResponseDTO result = authUserService.getAuthUserInfo(TEST_UID);

            // Then: Should update login count
            verify(repository).save(argThat(user ->
                    user.getTotalLoginCount() == 6 && user.getLastLoginTime() != null
            ));
            assertThat(result).isNotNull();
        }
    }

    // ========================================
    // createAuthUser Tests
    // ========================================

    @Nested
    @DisplayName("createAuthUser")
    class CreateAuthUser {

        @Test
        @DisplayName("Should throw DuplicateResourceException when email already exists")
        void shouldThrowDuplicateResourceExceptionWhenEmailExists() {
            // Given: Email already exists
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO("Test User", "existing@example.com");

            when(repository.existsByEmail(request.email())).thenReturn(true);

            // When & Then: Should throw DuplicateResourceException
            assertThatThrownBy(() -> authUserService.createAuthUser(TEST_UID, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");

            // And: Save should not be called
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should validate email before creating user")
        void shouldValidateEmailBeforeCreatingUser() {
            // Given: Valid request
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO("Test User", "test@example.com");
            when(repository.existsByEmail(request.email())).thenReturn(false);
            when(repository.save(any(AuthUserEntity.class))).thenAnswer(invocation -> {
                AuthUserEntity entity = invocation.getArgument(0);
                // Simulate @PrePersist behavior
                return AuthUserEntity.builder()
                        .uid(entity.getUid())
                        .name(entity.getName())
                        .email(entity.getEmail())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .lastLoginTime(entity.getLastLoginTime())
                        .totalLoginCount(entity.getTotalLoginCount())
                        .appReviewStatus(entity.getAppReviewStatus())
                        .build();
            });

            // When: Creating user
            authUserService.createAuthUser(TEST_UID, request);

            // Then: Should validate email first
            verify(repository).existsByEmail(request.email());
            verify(repository).save(any(AuthUserEntity.class));
        }
    }

    // ========================================
    // deleteAuthUser Tests
    // ========================================

    @Nested
    @DisplayName("deleteAuthUser")
    class DeleteAuthUser {

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not found")
        void shouldThrowEntityNotFoundExceptionWhenUserNotFound() {
            // Given: Repository returns empty
            when(repository.findById(TEST_UID))
                    .thenReturn(Optional.empty());

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> authUserService.deleteAuthUser(TEST_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Auth user not found");
        }

        @Test
        @DisplayName("Should nullify authUser reference before deleting")
        void shouldNullifyAuthUserReferenceBeforeDeleting() {
            // Given: User entities associated with auth user
            UserEntity user1 = UserEntity.builder().name("User 1").authUser(testAuthUser).build();
            UserEntity user2 = UserEntity.builder().name("User 2").authUser(testAuthUser).build();
            List<UserEntity> userEntities = Arrays.asList(user1, user2);

            when(repository.findById(TEST_UID)).thenReturn(Optional.of(testAuthUser));
            when(userRepository.findByAuthUserUid(TEST_UID)).thenReturn(userEntities);
            when(userRepository.saveAll(anyList())).thenReturn(userEntities);

            // When: Deleting auth user
            authUserService.deleteAuthUser(TEST_UID);

            // Then: Should nullify authUser references and save
            verify(userRepository).saveAll(argThat(users -> {
                if (users instanceof List) {
                    return ((List<UserEntity>) users).stream().allMatch(u -> u.getAuthUser() == null);
                }
                return false;
            }));
            verify(repository).deleteById(TEST_UID);
        }
    }

    // ========================================
    // updateAppReview Tests
    // ========================================

    @Nested
    @DisplayName("updateAppReview")
    class UpdateAppReview {

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not found")
        void shouldThrowEntityNotFoundExceptionWhenUserNotFound() {
            // Given: Repository returns empty
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            when(repository.findById(TEST_UID))
                    .thenReturn(Optional.empty());

            // When & Then: Should throw EntityNotFoundException
            assertThatThrownBy(() -> authUserService.updateAppReview(TEST_UID, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Auth user not found");

            // And: Save should not be called
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should update lastAppReviewDialogShownAt and status")
        void shouldUpdateLastAppReviewDialogShownAtAndStatus() {
            // Given: Existing user
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            when(repository.findById(TEST_UID)).thenReturn(Optional.of(testAuthUser));
            when(repository.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Updating app review
            AuthUserResponseDTO result = authUserService.updateAppReview(TEST_UID, request);

            // Then: Should update both fields
            verify(repository).save(argThat(user ->
                    user.getLastAppReviewDialogShownAt() != null &&
                            user.getAppReviewStatus() == AppReviewStatus.COMPLETED
            ));
            assertThat(result).isNotNull();
        }
    }
}