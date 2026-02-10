package com.tateca.tatecabackend.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID testUserId;
    private UserEntity testUser;
    private ListAppender<ILoggingEvent> logAppender;

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

        Logger logger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        logger.detachAppender(logAppender);
    }

    @Nested
    class RepositoryInteractionBehavior {

        @Test
        void shouldCallRepositoryMethodsInCorrectOrder() {
            // Given
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");
            when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updateUserName(testUserId, request);

            // Then
            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).findById(testUserId);
            inOrder.verify(repository).save(testUser);
        }

        @Test
        void shouldNotCallSaveWhenUserNotFound() {
            // Given
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");
            when(repository.findById(testUserId)).thenReturn(Optional.empty());

            // When
            try {
                userService.updateUserName(testUserId, request);
            } catch (EntityNotFoundException e) {
                // Expected
            }

            // Then
            verify(repository).findById(testUserId);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class EntityMutationBehavior {

        @Test
        void shouldMutateEntityNameBeforeSaving() {
            // Given
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Updated Name");
            when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updateUserName(testUserId, request);

            // Then
            assertThat(testUser.getName()).isEqualTo("Updated Name");
            verify(repository).save(testUser);
        }
    }

    @Nested
    class LoggingBehavior {

        @Test
        void shouldLogStartAndEndMessagesWithUserId() {
            // Given
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");
            when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updateUserName(testUserId, request);

            // Then - verify log messages
            assertThat(logAppender.list).hasSize(2);
            assertThat(logAppender.list.get(0).getFormattedMessage()).contains("Updating user name");
            assertThat(logAppender.list.get(1).getFormattedMessage()).contains("User name updated successfully");

            // Verify userId is included in log messages
            var messages = logAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();

            assertThat(messages)
                    .anyMatch(message -> message.contains("Updating user name") && message.contains("userId="));

            assertThat(messages)
                    .anyMatch(message -> message.contains("User name updated successfully") && message.contains("userId="));
        }

        @Test
        void shouldNotLogSuccessMessageWhenUpdateFails() {
            // Given
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");
            when(repository.findById(testUserId)).thenReturn(Optional.empty());

            // When
            try {
                userService.updateUserName(testUserId, request);
            } catch (EntityNotFoundException e) {
                // Expected
            }

            // Then
            assertThat(logAppender.list).hasSize(1);
            assertThat(logAppender.list.get(0).getFormattedMessage()).contains("Updating user name");
        }
    }
}
