package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.AuthUserAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.service.impl.AuthUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUserService Unit Tests")
class AuthUserServiceUnitTest {

    @Mock
    private AuthUserAccessor accessor;

    @Mock
    private UserAccessor userAccessor;

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
        @DisplayName("Should propagate NOT_FOUND exception from accessor")
        void shouldPropagateNotFoundExceptionFromAccessor() {
            // Given: Accessor throws NOT_FOUND exception
            when(accessor.findByUid(TEST_UID))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.getAuthUserInfo(TEST_UID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            // And: Save should not be called
            verify(accessor, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate INTERNAL_SERVER_ERROR exception from accessor save")
        void shouldPropagateInternalServerErrorFromAccessorSave() {
            // Given: Accessor save throws INTERNAL_SERVER_ERROR exception
            when(accessor.findByUid(TEST_UID)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.getAuthUserInfo(TEST_UID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Database error")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("Should update login count and timestamp")
        void shouldUpdateLoginCountAndTimestamp() {
            // Given: Existing user
            when(accessor.findByUid(TEST_UID)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Getting user info
            AuthUserResponseDTO result = authUserService.getAuthUserInfo(TEST_UID);

            // Then: Should update login count
            verify(accessor).save(argThat(user ->
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
        @DisplayName("Should propagate CONFLICT exception when email already exists")
        void shouldPropagateConflictExceptionWhenEmailExists() {
            // Given: Email validation throws CONFLICT exception
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO("Test User", "existing@example.com");

            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"))
                    .when(accessor).validateEmail(request.email());

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.createAuthUser(TEST_UID, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email already exists")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    });

            // And: Save should not be called
            verify(accessor, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate INTERNAL_SERVER_ERROR exception from accessor save")
        void shouldPropagateInternalServerErrorFromAccessorSave() {
            // Given: Save throws INTERNAL_SERVER_ERROR exception
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO("Test User", "test@example.com");

            doNothing().when(accessor).validateEmail(request.email());
            when(accessor.save(any(AuthUserEntity.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.createAuthUser(TEST_UID, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Database error")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("Should validate email before creating user")
        void shouldValidateEmailBeforeCreatingUser() {
            // Given: Valid request
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO("Test User", "test@example.com");
            doNothing().when(accessor).validateEmail(request.email());
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> {
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
            verify(accessor).validateEmail(request.email());
            verify(accessor).save(any(AuthUserEntity.class));
        }
    }

    // ========================================
    // deleteAuthUser Tests
    // ========================================

    @Nested
    @DisplayName("deleteAuthUser")
    class DeleteAuthUser {

        @Test
        @DisplayName("Should propagate NOT_FOUND exception from accessor")
        void shouldPropagateNotFoundExceptionFromAccessor() {
            // Given: Accessor throws NOT_FOUND exception
            when(accessor.findByUid(TEST_UID))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found with uid:" + TEST_UID));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.deleteAuthUser(TEST_UID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Auth User Not Found")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should propagate INTERNAL_SERVER_ERROR exception from accessor")
        void shouldPropagateInternalServerErrorFromAccessor() {
            // Given: Auth user exists but user accessor throws exception
            when(accessor.findByUid(TEST_UID)).thenReturn(testAuthUser);
            when(userAccessor.findByAuthUserUid(TEST_UID))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.deleteAuthUser(TEST_UID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Database error")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("Should nullify authUser reference before deleting")
        void shouldNullifyAuthUserReferenceBeforeDeleting() {
            // Given: User entities associated with auth user
            UserEntity user1 = UserEntity.builder().name("User 1").authUser(testAuthUser).build();
            UserEntity user2 = UserEntity.builder().name("User 2").authUser(testAuthUser).build();
            List<UserEntity> userEntities = Arrays.asList(user1, user2);

            when(accessor.findByUid(TEST_UID)).thenReturn(testAuthUser);
            when(userAccessor.findByAuthUserUid(TEST_UID)).thenReturn(userEntities);
            when(userAccessor.saveAll(anyList())).thenReturn(userEntities);
            doNothing().when(accessor).deleteById(TEST_UID);

            // When: Deleting auth user
            authUserService.deleteAuthUser(TEST_UID);

            // Then: Should nullify authUser references and save
            verify(userAccessor).saveAll(argThat(users ->
                    users.stream().allMatch(u -> u.getAuthUser() == null)
            ));
            verify(accessor).deleteById(TEST_UID);
        }
    }

    // ========================================
    // updateAppReview Tests
    // ========================================

    @Nested
    @DisplayName("updateAppReview")
    class UpdateAppReview {

        @Test
        @DisplayName("Should propagate NOT_FOUND exception from accessor")
        void shouldPropagateNotFoundExceptionFromAccessor() {
            // Given: Accessor throws NOT_FOUND exception
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            when(accessor.findByUid(TEST_UID))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.updateAppReview(TEST_UID, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            // And: Save should not be called
            verify(accessor, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate INTERNAL_SERVER_ERROR exception from accessor save")
        void shouldPropagateInternalServerErrorFromAccessorSave() {
            // Given: Save throws INTERNAL_SERVER_ERROR exception
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            when(accessor.findByUid(TEST_UID)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should propagate exception without catching
            assertThatThrownBy(() -> authUserService.updateAppReview(TEST_UID, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Database error")
                    .satisfies(exception -> {
                        ResponseStatusException rse = (ResponseStatusException) exception;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("Should update lastAppReviewDialogShownAt and status")
        void shouldUpdateLastAppReviewDialogShownAtAndStatus() {
            // Given: Existing user
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(AppReviewStatus.COMPLETED);

            when(accessor.findByUid(TEST_UID)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Updating app review
            AuthUserResponseDTO result = authUserService.updateAppReview(TEST_UID, request);

            // Then: Should update both fields
            verify(accessor).save(argThat(user ->
                    user.getLastAppReviewDialogShownAt() != null &&
                            user.getAppReviewStatus() == AppReviewStatus.COMPLETED
            ));
            assertThat(result).isNotNull();
        }
    }
}
