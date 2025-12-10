package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserInfoDTO;
import com.tateca.tatecabackend.dto.response.CurrencyNameDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerUnitTest extends AbstractControllerWebTest {

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        Mockito.reset(userService);
    }

    @Nested
    @DisplayName("PATCH /users/{userId}")
    class UpdateUserTests {

        @Test
        @DisplayName("Should return 200 OK when updating user name successfully")
        void shouldReturnOkWhenUpdatingUserName() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UserInfoDTO responseDTO = createUserInfoDTO(userId, "New Name", "JPY");

            when(userService.updateUserName(eq(userId), any(UpdateUserRequestDTO.class)))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.currency.currency_code").value("JPY"));

            verify(userService).updateUserName(eq(userId), any(UpdateUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when updating currency code successfully")
        void shouldReturnOkWhenUpdatingCurrencyCode() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UserInfoDTO responseDTO = createUserInfoDTO(userId, "Test User", "USD");

            when(userService.updateUserName(eq(userId), any(UpdateUserRequestDTO.class)))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currency_code": "USD"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(userId.toString()))
                    .andExpect(jsonPath("$.currency.currency_code").value("USD"));

            verify(userService).updateUserName(eq(userId), any(UpdateUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when updating both name and currency code")
        void shouldReturnOkWhenUpdatingBoth() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UserInfoDTO responseDTO = createUserInfoDTO(userId, "New Name", "EUR");

            when(userService.updateUserName(eq(userId), any(UpdateUserRequestDTO.class)))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name",
                                        "currency_code": "EUR"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.currency.currency_code").value("EUR"));

            verify(userService).updateUserName(eq(userId), any(UpdateUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user does not exist")
        void shouldReturnNotFoundWhenUserNotFound() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            when(userService.updateUserName(eq(userId), any(UpdateUserRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isNotFound());

            verify(userService).updateUserName(eq(userId), any(UpdateUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when currency code does not exist")
        void shouldReturnNotFoundWhenCurrencyNotFound() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            when(userService.updateUserName(eq(userId), any(UpdateUserRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found"));

            // When & Then - "XXX" is valid format (3 uppercase letters) but doesn't exist
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currency_code": "XXX"
                                    }
                                    """))
                    .andExpect(status().isNotFound());

            verify(userService).updateUserName(eq(userId), any(UpdateUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when userId is invalid UUID format")
        void shouldReturnBadRequestWhenInvalidUuid() throws Exception {
            // When & Then
            mockMvc.perform(patch("/users/{userId}", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateUserName(any(), any());
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when currency code format is invalid")
        void shouldReturnBadRequestWhenCurrencyCodeFormatInvalid() throws Exception {
            // When & Then - "INVALID" is not 3 uppercase letters
            mockMvc.perform(patch("/users/{userId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currency_code": "INVALID"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            // Service should never be called because validation fails at Controller layer
            verify(userService, never()).updateUserName(any(), any());
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            when(userService.updateUserName(eq(userId), any(UpdateUserRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(userService).updateUserName(eq(userId), any(UpdateUserRequestDTO.class));
        }
    }

    private UserInfoDTO createUserInfoDTO(UUID userId, String name, String currencyCode) {
        return new UserInfoDTO(
                userId.toString(),
                name,
                CurrencyNameDTO.builder()
                        .currencyCode(currencyCode)
                        .jpCurrencyName(currencyCode.equals("JPY") ? "日本円" : "米ドル")
                        .engCurrencyName(currencyCode.equals("JPY") ? "Japanese Yen" : "US Dollar")
                        .jpCountryName(currencyCode.equals("JPY") ? "日本" : "アメリカ")
                        .engCountryName(currencyCode.equals("JPY") ? "Japan" : "United States")
                        .currencySymbol(currencyCode.equals("JPY") ? "¥" : "$")
                        .build(),
                AuthUserInfoDTO.builder()
                        .uid("test-auth-uid")
                        .name("Test Auth User")
                        .email("test@example.com")
                        .build(),
                "2024-01-01T00:00:00+09:00",
                "2024-01-01T00:00:00+09:00"
        );
    }
}
