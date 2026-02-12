package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUserRepository authUserRepository;

    private UUID testUserId;
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        testAuthUser = TestFixtures.AuthUsers.defaultAuthUser();
        authUserRepository.save(testAuthUser);

        testUserId = UUID.randomUUID();
        UserEntity testUser = UserEntity.builder()
                .uuid(testUserId)
                .name("Original Name")
                .authUser(testAuthUser)
                .build();
        userRepository.save(testUser);

        flushAndClear();
    }

    @Test
    void givenUserExists_whenUpdatingName_thenShouldUpdateCorrectly() {
        // Given
        UserEntity originalUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        var originalUpdatedAt = originalUser.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Updated Name");

        // When
        UserResponseDTO result = userService.updateUserName(testUserId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(testUserId.toString());
        assertThat(result.userName()).isEqualTo("Updated Name");

        flushAndClear();
        UserEntity updatedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));

        // Verify name was updated
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");

        // Verify AuthUser relationship is preserved
        assertThat(updatedUser.getAuthUser()).isNotNull();
        assertThat(updatedUser.getAuthUser().getUid()).isEqualTo(testAuthUser.getUid());

        // Verify timestamp was updated
        assertThat(updatedUser.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void givenUserExists_whenUpdatingWithSameName_thenShouldSucceedIdempotently() {
        // Given
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Original Name");

        // When
        UserResponseDTO result = userService.updateUserName(testUserId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userName()).isEqualTo("Original Name");

        flushAndClear();
        UserEntity updatedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(updatedUser.getName()).isEqualTo("Original Name");
    }

    @Test
    void givenUserExists_whenUpdatingWithMinimumLengthName_thenShouldAccept() {
        // Given
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("A");

        // When
        UserResponseDTO result = userService.updateUserName(testUserId, request);

        // Then
        assertThat(result.userName()).isEqualTo("A");

        flushAndClear();
        UserEntity updatedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(updatedUser.getName()).isEqualTo("A");
    }

    @Test
    void givenUserExists_whenUpdatingWithMaximumLengthName_thenShouldAccept() {
        // Given
        String name50Chars = "A".repeat(50);
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO(name50Chars);

        // When
        UserResponseDTO result = userService.updateUserName(testUserId, request);

        // Then
        assertThat(result.userName()).isEqualTo(name50Chars);

        flushAndClear();
        UserEntity updatedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(updatedUser.getName()).isEqualTo(name50Chars);
    }

    @Test
    void givenUserExists_whenUpdatingWithSpecialCharacters_thenShouldPersistCorrectly() {
        // Given
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Test 😊 田中 €$");

        // When
        userService.updateUserName(testUserId, request);

        // Then
        flushAndClear();
        UserEntity updatedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(updatedUser.getName()).isEqualTo("Test 😊 田中 €$");
    }

    @Test
    void givenUserExists_whenUpdatingMultipleTimes_thenShouldPersistLatestValue() {
        // Given
        String[] names = {"Name1", "Name2", "Name3", "Name4", "Name5"};

        // When
        for (String name : names) {
            UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO(name);
            userService.updateUserName(testUserId, request);
        }
        flushAndClear();

        // Then
        UserEntity finalUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("User should exist"));
        assertThat(finalUser.getName()).isEqualTo("Name5");
    }

    @Test
    void givenUserDoesNotExist_whenUpdatingName_thenShouldThrowNotFoundException() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");
        long countBefore = userRepository.count();

        // When & Then
        assertThatThrownBy(() -> userService.updateUserName(nonExistentUserId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify no new user was created
        flushAndClear();
        long countAfter = userRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
