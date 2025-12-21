package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

@DisplayName("UserService Unit Tests")
class UserServiceUnitTest extends AbstractServiceUnitTest {

    @Mock
    private UserAccessor userAccessor;

    @Mock
    private CurrencyNameAccessor currencyNameAccessor;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID testUserId;
    private UserEntity testUser;
    private CurrencyNameEntity usdCurrency;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        CurrencyNameEntity jpyCurrency = TestFixtures.Currencies.jpy();
        usdCurrency = TestFixtures.Currencies.usd();

        testUser = UserEntity.builder()
                .uuid(testUserId)
                .name("Original Name")
                .currencyName(jpyCurrency)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("updateUserName")
    class UpdateUserNameTests {

        @Test
        @DisplayName("Should update user name only when only name is provided")
        void shouldUpdateNameOnly() {
            // Given
            UpdateUserRequestDTO request = new UpdateUserRequestDTO("New Name", null);

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserInfoDTO result = userService.updateUserName(testUserId, request);

            // Then
            assertThat(result.userName()).isEqualTo("New Name");
            verify(userAccessor).findById(testUserId);
            verify(userAccessor).save(testUser);
            verify(currencyNameAccessor, never()).findById(any());
        }

        @Test
        @DisplayName("Should update currency code only when only currency code is provided")
        void shouldUpdateCurrencyCodeOnly() {
            // Given
            UpdateUserRequestDTO request = new UpdateUserRequestDTO(null, "USD");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(currencyNameAccessor.findById("USD")).thenReturn(usdCurrency);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserInfoDTO result = userService.updateUserName(testUserId, request);

            // Then
            assertThat(result.currency().getCurrencyCode()).isEqualTo("USD");
            verify(userAccessor).findById(testUserId);
            verify(currencyNameAccessor).findById("USD");
            verify(userAccessor).save(testUser);
        }

        @Test
        @DisplayName("Should update both name and currency code when both are provided")
        void shouldUpdateBothNameAndCurrencyCode() {
            // Given
            UpdateUserRequestDTO request = new UpdateUserRequestDTO("New Name", "USD");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(currencyNameAccessor.findById("USD")).thenReturn(usdCurrency);
            when(userAccessor.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserInfoDTO result = userService.updateUserName(testUserId, request);

            // Then
            assertThat(result.userName()).isEqualTo("New Name");
            assertThat(result.currency().getCurrencyCode()).isEqualTo("USD");
            verify(userAccessor).findById(testUserId);
            verify(currencyNameAccessor).findById("USD");
            verify(userAccessor).save(testUser);
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when both name and currency code are null")
        void shouldThrowBadRequestWhenBothNull() {
            // Given
            UpdateUserRequestDTO request = new UpdateUserRequestDTO(null, null);

            // When & Then
            assertThatThrownBy(() -> userService.updateUserName(testUserId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });

            // Verify that accessor methods are never called
            verify(userAccessor, never()).findById(any());
            verify(userAccessor, never()).save(any());
            verify(currencyNameAccessor, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserNotFound() {
            // Given
            UpdateUserRequestDTO request = new UpdateUserRequestDTO("New Name", null);

            when(userAccessor.findById(testUserId))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then
            assertThatThrownBy(() -> userService.updateUserName(testUserId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(userAccessor).findById(testUserId);
            verify(userAccessor, never()).save(any());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when currency code does not exist")
        void shouldThrowNotFoundWhenCurrencyNotFound() {
            // Given
            UpdateUserRequestDTO request = new UpdateUserRequestDTO(null, "INVALID");

            when(userAccessor.findById(testUserId)).thenReturn(testUser);
            when(currencyNameAccessor.findById("INVALID"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found"));

            // When & Then
            assertThatThrownBy(() -> userService.updateUserName(testUserId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(exception -> {
                        ResponseStatusException ex = (ResponseStatusException) exception;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(userAccessor).findById(testUserId);
            verify(currencyNameAccessor).findById("INVALID");
            verify(userAccessor, never()).save(any());
        }
    }
}
