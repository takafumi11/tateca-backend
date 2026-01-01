package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("UserService Unit Tests")
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID testUserId;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        AuthUserEntity testAuthUser = TestFixtures.AuthUsers.defaultAuthUser();

        Instant testCreatedAt = Instant.now().minusSeconds(86400);
        Instant testUpdatedAt = Instant.now();

        testUser = UserEntity.builder()
                .uuid(testUserId)
                .name("Original Name")
                .authUser(testAuthUser)
                .createdAt(testCreatedAt)
                .updatedAt(testUpdatedAt)
                .build();
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found")
    void shouldThrowEntityNotFoundExceptionWhenUserNotFound() {
        // Given: Repository returns empty
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        when(repository.findById(testUserId))
                .thenReturn(Optional.empty());

        // When & Then: Should throw EntityNotFoundException
        assertThatThrownBy(() -> userService.updateUserName(testUserId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        // And: Save should not be called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update user name successfully")
    void shouldUpdateUserNameSuccessfully() {
        // Given: User exists
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Updating user name
        userService.updateUserName(testUserId, request);

        // Then: Should save with new name
        verify(repository).save(any(UserEntity.class));
        assertThat(testUser.getName()).isEqualTo("New Name");
    }
}
