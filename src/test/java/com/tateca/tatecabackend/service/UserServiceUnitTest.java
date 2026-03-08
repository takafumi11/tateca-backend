package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.exception.domain.ForbiddenException;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — updateUserName")
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl service;

    private static final String AUTH_UID = "firebase-uid-owner";
    private static final String OTHER_AUTH_UID = "firebase-uid-other";
    private static final UUID USER_ID = UUID.randomUUID();

    private UserEntity existingUser;

    @BeforeEach
    void setUp() {
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(AUTH_UID)
                .name("Owner")
                .email("owner@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        existingUser = UserEntity.builder()
                .uuid(USER_ID)
                .name("OldName")
                .authUser(authUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Given user does not exist")
    class WhenUserNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException")
        void shouldThrowEntityNotFoundException() {
            when(repository.findById(USER_ID)).thenReturn(Optional.empty());

            var request = new UpdateUserNameRequestDTO("NewName");

            assertThatThrownBy(() -> service.updateUserName(AUTH_UID, USER_ID, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("Given user exists but requester is not the resource owner")
    class WhenNotAuthorized {

        @Test
        @DisplayName("Should throw ForbiddenException")
        void shouldThrowForbiddenException() {
            when(repository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            var request = new UpdateUserNameRequestDTO("NewName");

            assertThatThrownBy(() -> service.updateUserName(OTHER_AUTH_UID, USER_ID, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_FORBIDDEN.getCode());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user has no auth user")
        void shouldThrowForbiddenExceptionWhenAuthUserIsNull() {
            UserEntity userWithoutAuth = UserEntity.builder()
                    .uuid(USER_ID)
                    .name("NoAuthUser")
                    .authUser(null)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            when(repository.findById(USER_ID)).thenReturn(Optional.of(userWithoutAuth));

            var request = new UpdateUserNameRequestDTO("NewName");

            assertThatThrownBy(() -> service.updateUserName(AUTH_UID, USER_ID, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_FORBIDDEN.getCode());

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Given authorized user updates with a different name")
    class WhenDifferentName {

        @Test
        @DisplayName("Should save and return updated user")
        void shouldSaveAndReturnUpdatedUser() {
            when(repository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
            when(repository.save(existingUser)).thenReturn(existingUser);

            var request = new UpdateUserNameRequestDTO("NewName");
            UserResponseDTO response = service.updateUserName(AUTH_UID, USER_ID, request);

            verify(repository).save(existingUser);
            assertThat(existingUser.getName()).isEqualTo("NewName");
            assertThat(response).isNotNull();
            assertThat(response.userName()).isEqualTo("NewName");
        }
    }

    @Nested
    @DisplayName("Given authorized user updates with the same name (idempotent)")
    class WhenSameName {

        @Test
        @DisplayName("Should NOT call save and return current state")
        void shouldSkipSaveAndReturnCurrentState() {
            when(repository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

            var request = new UpdateUserNameRequestDTO("OldName");
            UserResponseDTO response = service.updateUserName(AUTH_UID, USER_ID, request);

            verify(repository, never()).save(any());
            assertThat(response).isNotNull();
            assertThat(response.userName()).isEqualTo("OldName");
        }
    }
}
