package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.request.UpdateUserNameDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static final String BASE_ENDPOINT = "/users";

    @Nested
    @DisplayName("PATCH /users/{userId}")
    class UpdateUserNameEndpoint {

        @Test
        @DisplayName("Should return 200 OK and updated user info when update succeeds")
        void shouldReturn200WhenUpdateSucceeds() throws Exception {
            // Given: Valid request and service returns updated user
            UUID userId = UUID.randomUUID();
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("Updated Name");

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("Updated Name")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
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
            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should accept request with user_name field")
        void shouldAcceptRequestWithUserNameField() throws Exception {
            // Given: Valid request with user_name (JSON property name)
            UUID userId = UUID.randomUUID();
            String requestJson = """
                {
                    "user_name": "New Name"
                }
                """;

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("New Name")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When & Then: Should accept and process request
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"));

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should handle empty request body gracefully")
        void shouldHandleEmptyRequestBodyGracefully() throws Exception {
            // Given: Empty request body
            UUID userId = UUID.randomUUID();
            String emptyRequest = "{}";

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("Original Name")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When & Then: Should process empty request
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(userId.toString()));

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should handle null user_name in request")
        void shouldHandleNullUserNameInRequest() throws Exception {
            // Given: Request with null user_name
            UUID userId = UUID.randomUUID();
            String requestWithNull = """
                {
                    "user_name": null
                }
                """;

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("Original Name")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When & Then: Should process request with null value
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestWithNull))
                    .andExpect(status().isOk());

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should update with empty string name")
        void shouldUpdateWithEmptyStringName() throws Exception {
            // Given: Request with empty string
            UUID userId = UUID.randomUUID();
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("");

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When & Then: Should process empty string
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(""));

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should update with unicode characters")
        void shouldUpdateWithUnicodeCharacters() throws Exception {
            // Given: Request with Japanese characters
            UUID userId = UUID.randomUUID();
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("田中太郎");

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("田中太郎")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When & Then: Should handle unicode
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("田中太郎"));

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given: Service throws NOT_FOUND exception
            UUID userId = UUID.randomUUID();
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then: Should return 404
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should return 500 when internal server error occurs")
        void shouldReturn500WhenInternalServerError() throws Exception {
            // Given: Service throws internal server error
            UUID userId = UUID.randomUUID();
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should return 500
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").exists());

            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when invalid UUID format")
        void shouldReturn400WhenInvalidUUIDFormat() throws Exception {
            // Given: Invalid UUID format
            String invalidUUID = "not-a-valid-uuid";
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

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
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("New Name");

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

    @Nested
    @DisplayName("Service Integration")
    class ServiceIntegrationTests {

        @Test
        @DisplayName("Should pass correct userId to service")
        void shouldPassCorrectUserIdToService() throws Exception {
            // Given: Specific user ID
            UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("Test Name");

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("Test Name")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When: Calling endpoint
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then: Service should receive exact UUID
            verify(userService, times(1)).updateUserName(eq(userId), any(UpdateUserNameDTO.class));
        }

        @Test
        @DisplayName("Should pass correct DTO to service")
        void shouldPassCorrectDTOToService() throws Exception {
            // Given: Request with specific name
            UUID userId = UUID.randomUUID();
            UpdateUserNameDTO request = new UpdateUserNameDTO();
            request.setName("Specific Name");

            UserInfoDTO expectedResponse = UserInfoDTO.builder()
                    .uuid(userId.toString())
                    .userName("Specific Name")
                    .createdAt("2024-01-01T09:00:00+09:00")
                    .updatedAt("2024-01-15T09:00:00+09:00")
                    .build();

            when(userService.updateUserName(eq(userId), any(UpdateUserNameDTO.class)))
                    .thenReturn(expectedResponse);

            // When: Calling endpoint
            mockMvc.perform(patch(BASE_ENDPOINT + "/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then: Service should receive DTO with correct name
            verify(userService, times(1)).updateUserName(
                    eq(userId),
                    argThat(dto -> "Specific Name".equals(dto.getName()))
            );
        }
    }
}
