package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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
    private UserAccessor userAccessor;

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
    @DisplayName("Should propagate NOT_FOUND exception from accessor")
    void shouldPropagateNotFoundExceptionFromAccessor() {
        // Given: Accessor throws NOT_FOUND exception
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        when(userAccessor.findById(testUserId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // When & Then: Should propagate exception without catching
        assertThatThrownBy(() -> userService.updateUserName(testUserId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found")
                .satisfies(exception -> {
                    ResponseStatusException rse = (ResponseStatusException) exception;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        // And: Save should not be called
        verify(userAccessor, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate INTERNAL_SERVER_ERROR exception from accessor save")
    void shouldPropagateInternalServerErrorFromAccessorSave() {
        // Given: Accessor save throws INTERNAL_SERVER_ERROR exception
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        when(userAccessor.findById(testUserId)).thenReturn(testUser);
        when(userAccessor.save(any(UserEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

        // When & Then: Should propagate exception without catching
        assertThatThrownBy(() -> userService.updateUserName(testUserId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Database error")
                .satisfies(exception -> {
                    ResponseStatusException rse = (ResponseStatusException) exception;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}
