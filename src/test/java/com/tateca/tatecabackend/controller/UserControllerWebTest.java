package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("UserController Web Tests")
class UserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static final String BASE_ENDPOINT = "/users";

    @Test
    @DisplayName("Should return 200 OK and updated user info when update succeeds")
    void shouldReturn200WhenUpdateSucceeds() throws Exception {
        // Given: Valid request and service returns updated user
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("Updated Name");

        UserResponseDTO expectedResponse = new UserResponseDTO(
                userId.toString(),
                "Updated Name",
                null,
                "2024-01-01T09:00:00+09:00",
                "2024-01-15T09:00:00+09:00"
        );

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenReturn(expectedResponse);

        // When & Then: Should return 200 with updated user info
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uuid").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists());

        // And: Service should be called once
        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when user_name is missing")
    void shouldReturn400WhenUserNameIsMissing() throws Exception {
        // Given: Empty request body (user_name missing)
        UUID userId = UUID.randomUUID();
        String emptyRequest = "{}";

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyRequest))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when user_name is null")
    void shouldReturn400WhenUserNameIsNull() throws Exception {
        // Given: Request with null user_name
        UUID userId = UUID.randomUUID();
        String requestWithNull = """
            {
                "user_name": null
            }
            """;

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithNull))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when user_name is empty string")
    void shouldReturn400WhenUserNameIsEmpty() throws Exception {
        // Given: Request with empty string
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("");

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when user_name is only whitespace")
    void shouldReturn400WhenUserNameIsOnlyWhitespace() throws Exception {
        // Given: Request with only whitespace
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("   ");

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should update with unicode characters")
    void shouldUpdateWithUnicodeCharacters() throws Exception {
        // Given: Request with Japanese characters
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("田中太郎");

        UserResponseDTO expectedResponse = new UserResponseDTO(
                userId.toString(),
                "田中太郎",
                null,
                "2024-01-01T09:00:00+09:00",
                "2024-01-15T09:00:00+09:00"
        );

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenReturn(expectedResponse);

        // When & Then: Should handle unicode
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("田中太郎"));

        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should accept name with exactly 1 character")
    void shouldAcceptNameWith1Character() throws Exception {
        // Given: Request with 1 character (minimum boundary value)
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("A");

        UserResponseDTO expectedResponse = new UserResponseDTO(
                userId.toString(),
                "A",
                null,
                "2024-01-01T09:00:00+09:00",
                "2024-01-15T09:00:00+09:00"
        );

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenReturn(expectedResponse);

        // When & Then: Should accept minimum length name
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("A"));

        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should accept name with exactly 50 characters")
    void shouldAcceptNameWith50Characters() throws Exception {
        // Given: Request with 50 characters (boundary value)
        UUID userId = UUID.randomUUID();
        String name50Chars = "A".repeat(50);
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO(name50Chars);

        UserResponseDTO expectedResponse = new UserResponseDTO(
                userId.toString(),
                name50Chars,
                null,
                "2024-01-01T09:00:00+09:00",
                "2024-01-15T09:00:00+09:00"
        );

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenReturn(expectedResponse);

        // When & Then: Should accept maximum length name
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name50Chars));

        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when name exceeds 50 characters")
    void shouldReturn400WhenNameExceeds50Characters() throws Exception {
        // Given: Request with 51 characters (over limit)
        UUID userId = UUID.randomUUID();
        String name51Chars = "A".repeat(51);
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO(name51Chars);

        // When & Then: Should return 400 due to @Size validation
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should accept name with emoji characters")
    void shouldAcceptNameWithEmoji() throws Exception {
        // Given: Request with emoji characters
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("John 😊 Doe");

        UserResponseDTO expectedResponse = new UserResponseDTO(
                userId.toString(),
                "John 😊 Doe",
                null,
                "2024-01-01T09:00:00+09:00",
                "2024-01-15T09:00:00+09:00"
        );

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenReturn(expectedResponse);

        // When & Then: Should handle emoji
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John 😊 Doe"));

        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when request body is completely missing")
    void shouldReturn400WhenRequestBodyMissing() throws Exception {
        // Given: Request without body
        UUID userId = UUID.randomUUID();

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Given: Service throws NOT_FOUND exception
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // When & Then: Should return 404
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should return 500 when internal server error occurs")
    void shouldReturn500WhenInternalServerError() throws Exception {
        // Given: Service throws internal server error
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        when(userService.updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

        // When & Then: Should return 500
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameRequestDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when invalid UUID format")
    void shouldReturn400WhenInvalidUUIDFormat() throws Exception {
        // Given: Invalid UUID format
        String invalidUUID = "not-a-valid-uuid";
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", invalidUUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Service should not be called with invalid UUID
        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when Content-Type is missing")
    void shouldReturn400WhenContentTypeIsMissing() throws Exception {
        // Given: Request without Content-Type header
        UUID userId = UUID.randomUUID();
        UpdateUserNameRequestDTO request = new UpdateUserNameRequestDTO("New Name");

        // When & Then: Should return 415 Unsupported Media Type
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).updateUserName(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when request body is malformed JSON")
    void shouldReturn400WhenRequestBodyIsMalformedJSON() throws Exception {
        // Given: Malformed JSON
        UUID userId = UUID.randomUUID();
        String malformedJson = "{ invalid json }";

        // When & Then: Should return 400
        mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserName(any(), any());
    }
}
