package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserNameDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceUnitTest {

    @Mock
    private UserAccessor userAccessor;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<UserEntity> userEntityCaptor;

    private UUID testUserId;
    private UserEntity testUser;
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        Instant testCreatedAt = Instant.now().minusSeconds(86400); // 1 day ago
        Instant testUpdatedAt = Instant.now();

        testAuthUser = AuthUserEntity.builder()
                .uid("test-user-uid")
                .name("Test Auth User")
                .email("test@example.com")
                .createdAt(testCreatedAt)
                .updatedAt(testUpdatedAt)
                .build();

        testUser = UserEntity.builder()
                .uuid(testUserId)
                .name("Original Name")
                .authUser(testAuthUser)
                .createdAt(testCreatedAt)
                .updatedAt(testUpdatedAt)
                .build();
    }

    @Nested
    @DisplayName("Given user exists")
    class WhenUserExists {

        @Test
        @DisplayName("Then should update user name when name is provided")
        void thenShouldUpdateUserNameWhenNameProvided() {
            // Given: User exists and new name is provided
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Updating user name
            userService.updateUserName(testUserId, request);

            // Then: Should call findById once
            verify(userAccessor, times(1)).findById(testUserId);

            // And: Should save user with new name
            verify(userAccessor, times(1)).save(userEntityCaptor.capture());
            UserEntity savedUser = userEntityCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("Then should not update when name is null")
        void thenShouldNotUpdateWhenNameIsNull() {
            // Given: User exists and name is null
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName(null);

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Updating user name
            userService.updateUserName(testUserId, request);

            // Then: Should call findById once
            verify(userAccessor, times(1)).findById(testUserId);

            // And: Should save user without changing name
            verify(userAccessor, times(1)).save(userEntityCaptor.capture());
            UserEntity savedUser = userEntityCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("Original Name");
        }

        @Test
        @DisplayName("Then should update name with empty string")
        void thenShouldUpdateNameWithEmptyString() {
            // Given: User exists and name is empty string
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Updating user name
            userService.updateUserName(testUserId, request);

            // Then: Should save user with empty name
            verify(userAccessor, times(1)).save(userEntityCaptor.capture());
            UserEntity savedUser = userEntityCaptor.getValue();
            assertThat(savedUser.getName()).isEmpty();
        }

        @Test
        @DisplayName("Then should preserve other user properties")
        void thenShouldPreserveOtherUserProperties() {
            // Given: User exists with authUser
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            UUID originalUuid = testUser.getUuid();
            AuthUserEntity originalAuthUser = testUser.getAuthUser();
            Instant originalCreatedAt = testUser.getCreatedAt();
            Instant originalUpdatedAt = testUser.getUpdatedAt();

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Updating user name
            userService.updateUserName(testUserId, request);

            // Then: Should preserve UUID, authUser, timestamps
            verify(userAccessor, times(1)).save(userEntityCaptor.capture());
            UserEntity savedUser = userEntityCaptor.getValue();
            assertThat(savedUser.getUuid()).isEqualTo(originalUuid);
            assertThat(savedUser.getAuthUser()).isEqualTo(originalAuthUser);
            assertThat(savedUser.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(savedUser.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Given accessor operations")
    class WhenAccessorOperations {

        @Test
        @DisplayName("Then should call findById exactly once")
        void thenShouldCallFindByIdExactlyOnce() {
            // Given
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updateUserName(testUserId, request);

            // Then: Should call findById exactly once
            verify(userAccessor, times(1)).findById(testUserId);
        }

        @Test
        @DisplayName("Then should call save exactly once")
        void thenShouldCallSaveExactlyOnce() {
            // Given
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updateUserName(testUserId, request);

            // Then: Should call save exactly once
            verify(userAccessor, times(1)).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Then should save the same entity instance returned from findById")
        void thenShouldSaveSameEntityInstance() {
            // Given
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updateUserName(testUserId, request);

            // Then: Should save the same entity instance
            verify(userAccessor, times(1)).save(userEntityCaptor.capture());
            UserEntity savedUser = userEntityCaptor.getValue();
            assertThat(savedUser).isSameAs(testUser);
        }
    }

    @Nested
    @DisplayName("Given UserInfoDTO conversion")
    class WhenConvertingToUserInfoDTO {

        @Test
        @DisplayName("Then should return DTO with updated user data")
        void thenShouldReturnDTOWithUpdatedUserData() {
            // Given
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("Updated Name");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity user = invocation.getArgument(0);
                return user;
            });

            // When
            UserInfoDTO result = userService.updateUserName(testUserId, request);

            // Then: Should return DTO with correct data
            assertThat(result).isNotNull();
            assertThat(result.getUuid()).isEqualTo(testUserId.toString());
            assertThat(result.getUserName()).isEqualTo("Updated Name");
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }
}
