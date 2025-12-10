package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.AuthUserAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.AuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserInfoDTO;
import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthUserService Unit Tests")
class AuthUserServiceUnitTest extends AbstractServiceUnitTest {

    @Mock
    private AuthUserAccessor accessor;

    @Mock
    private UserAccessor userAccessor;

    @InjectMocks
    private AuthUserService authUserService;

    private String testUid;
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        testUid = "test-auth-uid";
        testAuthUser = TestFixtures.AuthUsers.standard();
        testAuthUser.setUid(testUid);
        testAuthUser.setTotalLoginCount(5);
        testAuthUser.setCreatedAt(Instant.now());
        testAuthUser.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("getAuthUserInfo")
    class GetAuthUserInfoTests {

        @Test
        @DisplayName("Should retrieve auth user, increment login count, and update last login time")
        void shouldGetAuthUserInfoSuccessfully() {
            // Given
            Instant beforeCall = Instant.now();
            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthUserInfoDTO result = authUserService.getAuthUserInfo(testUid);

            // Then
            assertThat(result.getUid()).isEqualTo(testUid);
            assertThat(result.getTotalLoginCount()).isEqualTo(6); // Incremented from 5 to 6

            // Verify accessor calls
            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getTotalLoginCount()).isEqualTo(6);
            assertThat(savedEntity.getLastLoginTime()).isAfter(beforeCall);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserNotFound() {
            // Given
            when(accessor.findByUid(testUid))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found"));

            // When & Then
            assertThatThrownBy(() -> authUserService.getAuthUserInfo(testUid))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(accessor).findByUid(testUid);
            verify(accessor, never()).save(any());
        }

        @Test
        @DisplayName("Should handle zero login count correctly")
        void shouldHandleZeroLoginCount() {
            // Given
            testAuthUser.setTotalLoginCount(0);
            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthUserInfoDTO result = authUserService.getAuthUserInfo(testUid);

            // Then
            assertThat(result.getTotalLoginCount()).isEqualTo(1); // Incremented from 0 to 1

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());
            assertThat(captor.getValue().getTotalLoginCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("createAuthUser")
    class CreateAuthUserTests {

        private AuthUserRequestDTO validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new AuthUserRequestDTO();
            validRequest.setName("New User");
            validRequest.setEmail("newuser@example.com");
        }

        @Test
        @DisplayName("Should create auth user with valid request")
        void shouldCreateAuthUserSuccessfully() {
            // Given
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> {
                AuthUserEntity entity = invocation.getArgument(0);
                // Simulate @PrePersist behavior
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                return entity;
            });

            // When
            AuthUserInfoDTO result = authUserService.createAuthUser(testUid, validRequest);

            // Then
            assertThat(result.getUid()).isEqualTo(testUid);
            assertThat(result.getName()).isEqualTo("New User");
            assertThat(result.getEmail()).isEqualTo("newuser@example.com");
            assertThat(result.getTotalLoginCount()).isEqualTo(1);
            assertThat(result.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);

            // Verify accessor calls
            verify(accessor).validateEmail("newuser@example.com");

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getUid()).isEqualTo(testUid);
            assertThat(savedEntity.getName()).isEqualTo("New User");
            assertThat(savedEntity.getEmail()).isEqualTo("newuser@example.com");
            assertThat(savedEntity.getTotalLoginCount()).isEqualTo(1);
            assertThat(savedEntity.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
            assertThat(savedEntity.getLastLoginTime()).isNotNull();
        }

        @Test
        @DisplayName("Should throw CONFLICT when email already exists")
        void shouldThrowConflictWhenEmailExists() {
            // Given
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"))
                    .when(accessor).validateEmail(anyString());

            // When & Then
            assertThatThrownBy(() -> authUserService.createAuthUser(testUid, validRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    });

            verify(accessor).validateEmail("newuser@example.com");
            verify(accessor, never()).save(any());
        }

        @Test
        @DisplayName("Should initialize appReviewStatus to PENDING")
        void shouldInitializeAppReviewStatusToPending() {
            // Given
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> {
                AuthUserEntity entity = invocation.getArgument(0);
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                return entity;
            });

            // When
            AuthUserInfoDTO result = authUserService.createAuthUser(testUid, validRequest);

            // Then
            assertThat(result.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());
            assertThat(captor.getValue().getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
        }

        @Test
        @DisplayName("Should set totalLoginCount to 1 on creation")
        void shouldSetLoginCountToOne() {
            // Given
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> {
                AuthUserEntity entity = invocation.getArgument(0);
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                return entity;
            });

            // When
            AuthUserInfoDTO result = authUserService.createAuthUser(testUid, validRequest);

            // Then
            assertThat(result.getTotalLoginCount()).isEqualTo(1);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());
            assertThat(captor.getValue().getTotalLoginCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteAuthUser")
    class DeleteAuthUserTests {

        @Test
        @DisplayName("Should delete auth user and nullify linked UserEntity.authUser - with linked users")
        void shouldDeleteAuthUserAndNullifyLinkedUsers() {
            // Given
            UserEntity linkedUser1 = TestFixtures.Users.standard();
            linkedUser1.setAuthUser(testAuthUser);

            UserEntity linkedUser2 = TestFixtures.Users.standard();
            linkedUser2.setAuthUser(testAuthUser);

            List<UserEntity> linkedUsers = List.of(linkedUser1, linkedUser2);

            when(userAccessor.findByAuthUserUid(testUid)).thenReturn(linkedUsers);
            when(userAccessor.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authUserService.deleteAuthUser(testUid);

            // Then
            // Verify forEach: All users have authUser set to null
            assertThat(linkedUser1.getAuthUser()).isNull();
            assertThat(linkedUser2.getAuthUser()).isNull();

            verify(userAccessor).findByAuthUserUid(testUid);

            ArgumentCaptor<List<UserEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(userAccessor).saveAll(captor.capture());
            List<UserEntity> savedUsers = captor.getValue();
            assertThat(savedUsers).hasSize(2);
            assertThat(savedUsers).allMatch(user -> user.getAuthUser() == null);

            verify(accessor).deleteById(testUid);
        }

        @Test
        @DisplayName("Should delete auth user successfully when no linked users - empty forEach")
        void shouldDeleteAuthUserWhenNoLinkedUsers() {
            // Given
            when(userAccessor.findByAuthUserUid(testUid)).thenReturn(Collections.emptyList());
            when(userAccessor.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authUserService.deleteAuthUser(testUid);

            // Then
            // Verify forEach: Empty list means saveAll is called with empty list
            verify(userAccessor).findByAuthUserUid(testUid);

            ArgumentCaptor<List<UserEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(userAccessor).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();

            verify(accessor).deleteById(testUid);
        }

        @Test
        @DisplayName("Should delete auth user even when only one linked user exists")
        void shouldDeleteAuthUserWithSingleLinkedUser() {
            // Given
            UserEntity singleUser = TestFixtures.Users.standard();
            singleUser.setAuthUser(testAuthUser);

            when(userAccessor.findByAuthUserUid(testUid)).thenReturn(List.of(singleUser));
            when(userAccessor.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authUserService.deleteAuthUser(testUid);

            // Then
            assertThat(singleUser.getAuthUser()).isNull();

            verify(userAccessor).findByAuthUserUid(testUid);

            ArgumentCaptor<List<UserEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(userAccessor).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);

            verify(accessor).deleteById(testUid);
        }
    }

    @Nested
    @DisplayName("updateAppReview")
    class UpdateAppReviewTests {

        @Test
        @DisplayName("Should update lastAppReviewDialogShownAt when showDialog is true")
        void shouldUpdateDialogTimestampWhenShowDialogTrue() {
            // Given
            Instant beforeCall = Instant.now();
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(true);
            request.setAppReviewStatus(null);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthUserInfoDTO result = authUserService.updateAppReview(testUid, request);

            // Then
            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getLastAppReviewDialogShownAt()).isAfter(beforeCall);
        }

        @Test
        @DisplayName("Should NOT update lastAppReviewDialogShownAt when showDialog is false")
        void shouldNotUpdateDialogTimestampWhenShowDialogFalse() {
            // Given
            testAuthUser.setLastAppReviewDialogShownAt(null);
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(false);
            request.setAppReviewStatus(null);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authUserService.updateAppReview(testUid, request);

            // Then
            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            // Verify lastAppReviewDialogShownAt remains null (not updated)
            assertThat(savedEntity.getLastAppReviewDialogShownAt()).isNull();
        }

        @Test
        @DisplayName("Should update appReviewStatus when appReviewStatus is not null")
        void shouldUpdateAppReviewStatusWhenNotNull() {
            // Given
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(false);
            request.setAppReviewStatus(AppReviewStatus.COMPLETED);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthUserInfoDTO result = authUserService.updateAppReview(testUid, request);

            // Then
            assertThat(result.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);

            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should NOT update appReviewStatus when appReviewStatus is null")
        void shouldNotUpdateAppReviewStatusWhenNull() {
            // Given
            testAuthUser.setAppReviewStatus(AppReviewStatus.PENDING);
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(false);
            request.setAppReviewStatus(null);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authUserService.updateAppReview(testUid, request);

            // Then
            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            // Verify appReviewStatus remains PENDING (not updated)
            assertThat(savedEntity.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
        }

        @Test
        @DisplayName("Should update both fields when showDialog=true and appReviewStatus=COMPLETED")
        void shouldUpdateBothFieldsWhenBothProvided() {
            // Given
            Instant beforeCall = Instant.now();
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(true);
            request.setAppReviewStatus(AppReviewStatus.COMPLETED);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthUserInfoDTO result = authUserService.updateAppReview(testUid, request);

            // Then
            assertThat(result.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);

            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getLastAppReviewDialogShownAt()).isAfter(beforeCall);
            assertThat(savedEntity.getAppReviewStatus()).isEqualTo(AppReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should update neither field when showDialog=false and appReviewStatus=null")
        void shouldUpdateNeitherFieldWhenBothFalse() {
            // Given
            testAuthUser.setLastAppReviewDialogShownAt(null);
            testAuthUser.setAppReviewStatus(AppReviewStatus.PENDING);
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(false);
            request.setAppReviewStatus(null);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authUserService.updateAppReview(testUid, request);

            // Then
            verify(accessor).findByUid(testUid);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());

            AuthUserEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getLastAppReviewDialogShownAt()).isNull();
            assertThat(savedEntity.getAppReviewStatus()).isEqualTo(AppReviewStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserNotFound() {
            // Given
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(true);
            request.setAppReviewStatus(AppReviewStatus.COMPLETED);

            when(accessor.findByUid(testUid))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found"));

            // When & Then
            assertThatThrownBy(() -> authUserService.updateAppReview(testUid, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(accessor).findByUid(testUid);
            verify(accessor, never()).save(any());
        }

        @Test
        @DisplayName("Should handle appReviewStatus=PERMANENTLY_DECLINED correctly")
        void shouldHandlePermanentlyDeclinedStatus() {
            // Given
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO();
            request.setShowDialog(false);
            request.setAppReviewStatus(AppReviewStatus.PERMANENTLY_DECLINED);

            when(accessor.findByUid(testUid)).thenReturn(testAuthUser);
            when(accessor.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthUserInfoDTO result = authUserService.updateAppReview(testUid, request);

            // Then
            assertThat(result.getAppReviewStatus()).isEqualTo(AppReviewStatus.PERMANENTLY_DECLINED);

            ArgumentCaptor<AuthUserEntity> captor = ArgumentCaptor.forClass(AuthUserEntity.class);
            verify(accessor).save(captor.capture());
            assertThat(captor.getValue().getAppReviewStatus()).isEqualTo(AppReviewStatus.PERMANENTLY_DECLINED);
        }
    }
}
