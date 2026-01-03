package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.service.AuthUserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthUserController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("AuthUserController Web Tests")
class AuthUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthUserService authUserService;

    private static final String BASE_ENDPOINT = "/auth/users";
    private static final String TEST_UID = TestSecurityConfig.TEST_UID;

    // ========================================
    // GET /auth/users/{uid}
    // ========================================

    @Nested
    @DisplayName("GET /auth/users/{uid}")
    class GetAuthUser {

        @Test
        @DisplayName("Should return 200 OK when getting user with valid uid")
        void shouldReturn200WhenGetWithValidUid() throws Exception {
            // Given: Valid uid
            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "John Doe",
                "john@example.com",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                5,
                null,
                AppReviewStatus.PENDING
            );

            when(authUserService.getAuthUserInfo(TEST_UID))
                .thenReturn(expectedResponse);

            // When & Then: Should return 200 OK
            mockMvc.perform(get(BASE_ENDPOINT + "/{uid}", TEST_UID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uid").value(TEST_UID))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

            verify(authUserService, times(1)).getAuthUserInfo(TEST_UID);
        }

        @Test
        @DisplayName("Should return 200 OK when uid is exactly 128 characters")
        void shouldReturn200WhenUidIsExactly128Characters() throws Exception {
            // Given: UID with exactly 128 characters
            String uid128Chars = "A".repeat(128);
            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                uid128Chars,
                "John Doe",
                "john@example.com",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                5,
                null,
                AppReviewStatus.PENDING
            );

            when(authUserService.getAuthUserInfo(uid128Chars))
                .thenReturn(expectedResponse);

            // When & Then: Should return 200 OK
            mockMvc.perform(get(BASE_ENDPOINT + "/{uid}", uid128Chars))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(uid128Chars));

            verify(authUserService, times(1)).getAuthUserInfo(uid128Chars);
        }

        @Test
        @DisplayName("Should return 400 when uid exceeds 128 characters")
        void shouldReturn400WhenUidExceeds128Characters() throws Exception {
            // Given: UID with 129 characters
            String uid129Chars = "A".repeat(129);

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(get(BASE_ENDPOINT + "/{uid}", uid129Chars))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("UID must not exceed 128 characters"));

            // And: Service should NOT be called
            verify(authUserService, never()).getAuthUserInfo(any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given: Service throws NOT_FOUND exception
            when(authUserService.getAuthUserInfo(TEST_UID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then: Should return 404 NOT_FOUND
            mockMvc.perform(get(BASE_ENDPOINT + "/{uid}", TEST_UID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));

            verify(authUserService, times(1)).getAuthUserInfo(TEST_UID);
        }
    }

    // ========================================
    // POST /auth/users
    // ========================================

    @Nested
    @DisplayName("POST /auth/users")
    class CreateAuthUser {

        // ========== Success Scenarios (2 tests) - HTTP 201 CREATED ==========

        @Test
        @DisplayName("Should return 201 CREATED when creating user with valid data")
        void shouldReturn201WhenCreateWithValidData() throws Exception {
            // Given: Valid request
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                "john@example.com"
            );

            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "",
                "john@example.com",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                1,
                null,
                AppReviewStatus.PENDING
            );

            when(authUserService.createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class)))
                .thenReturn(expectedResponse);

            // When & Then: Should return 201 CREATED
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uid").value(TEST_UID))
                .andExpect(jsonPath("$.name").value(""))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists())
                .andExpect(jsonPath("$.last_login_time").exists())
                .andExpect(jsonPath("$.total_login_count").value(1))
                .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            // And: Service should be called once
            verify(authUserService, times(1))
                .createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 when email is exactly 255 characters")
        void shouldReturn201WhenEmailIsExactly255Characters() throws Exception {
            // Given: Request with email of exactly 255 characters
            String email255Chars = "a".repeat(243) + "@example.com"; // 243 + 12 = 255
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                email255Chars
            );

            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "",
                email255Chars,
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                1,
                null,
                AppReviewStatus.PENDING
            );

            when(authUserService.createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class)))
                .thenReturn(expectedResponse);

            // When & Then: Should return 201 CREATED
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email255Chars));

            verify(authUserService, times(1))
                .createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class));
        }

        // ========== Validation Failures (5 tests) - HTTP 400 BAD_REQUEST ==========

        @Test
        @DisplayName("Should return 400 when email is null")
        void shouldReturn400WhenEmailIsNull() throws Exception {
            // Given: Request with null email
            String requestWithNull = "{\"email\": null}";

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestWithNull))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when email is empty string")
        void shouldReturn400WhenEmailIsEmpty() throws Exception {
            // Given: Request with empty email
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                ""
            );

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when email is whitespace only")
        void shouldReturn400WhenEmailIsWhitespaceOnly() throws Exception {
            // Given: Request with whitespace-only email
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                "   "
            );

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when email exceeds 255 characters")
        void shouldReturn400WhenEmailExceeds255Characters() throws Exception {
            // Given: Request with email of 256 characters
            String longEmail = "a".repeat(244) + "@example.com"; // 256 chars total
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                longEmail
            );

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when email field is missing")
        void shouldReturn400WhenEmailFieldMissing() throws Exception {
            // Given: Request JSON without email field
            String requestMissingEmail = "{}";

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestMissingEmail))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        // ========== HTTP Layer Failures (3 tests) ==========

        @Test
        @DisplayName("Should return 400 when request body is completely missing")
        void shouldReturn400WhenRequestBodyMissing() throws Exception {
            // Given: No request body
            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when request body is malformed JSON")
        void shouldReturn400WhenRequestBodyIsMalformedJSON() throws Exception {
            // Given: Malformed JSON
            String malformedJson = "{ invalid json }";

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is missing")
        void shouldReturn415WhenContentTypeIsMissing() throws Exception {
            // Given: Valid JSON but no Content-Type header
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                "test@example.com"
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

            verify(authUserService, never()).createAuthUser(any(), any());
        }

        // ========== Business Logic Failures (2 tests) ==========

        @Test
        @DisplayName("Should return 409 when email already exists")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            // Given: Service throws CONFLICT exception
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                "existing@example.com"
            );

            when(authUserService.createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

            // When & Then: Should return 409 CONFLICT
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));

            // And: Service should be called
            verify(authUserService, times(1))
                .createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 when internal server error occurs")
        void shouldReturn500WhenInternalServerError() throws Exception {
            // Given: Service throws INTERNAL_SERVER_ERROR exception
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                "john@example.com"
            );

            when(authUserService.createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should return 500 INTERNAL_SERVER_ERROR
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database error"));

            // And: Service should be called
            verify(authUserService, times(1))
                .createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class));
        }

        // ========== Edge Cases (1 test) ==========

        @Test
        @DisplayName("Should return 201 when email has leading/trailing whitespace")
        void shouldReturn201WhenEmailHasLeadingTrailingWhitespace() throws Exception {
            // Given: Request with email having whitespace
            CreateAuthUserRequestDTO request = new CreateAuthUserRequestDTO(
                "  user@example.com  "
            );

            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "",
                "  user@example.com  ",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                1,
                null,
                AppReviewStatus.PENDING
            );

            when(authUserService.createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class)))
                .thenReturn(expectedResponse);

            // When & Then: Should return 201 CREATED
            mockMvc.perform(post(BASE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("  user@example.com  "));

            verify(authUserService, times(1))
                .createAuthUser(eq(TEST_UID), any(CreateAuthUserRequestDTO.class));
        }
    }

    // ========================================
    // DELETE /auth/users/{uid}
    // ========================================

    @Nested
    @DisplayName("DELETE /auth/users/{uid}")
    class DeleteAuthUser {

        @Test
        @DisplayName("Should return 204 NO_CONTENT when deleting user successfully")
        void shouldReturn204WhenDeleteSuccessful() throws Exception {
            // Given: Service completes successfully
            doNothing().when(authUserService).deleteAuthUser(TEST_UID);

            // When & Then: Should return 204 NO_CONTENT
            mockMvc.perform(delete(BASE_ENDPOINT + "/{uid}", TEST_UID))
                .andExpect(status().isNoContent());

            verify(authUserService, times(1)).deleteAuthUser(TEST_UID);
        }

        @Test
        @DisplayName("Should return 204 NO_CONTENT when uid is exactly 128 characters")
        void shouldReturn204WhenUidIsExactly128Characters() throws Exception {
            // Given: UID with exactly 128 characters
            String uid128Chars = "A".repeat(128);
            doNothing().when(authUserService).deleteAuthUser(uid128Chars);

            // When & Then: Should return 204 NO_CONTENT
            mockMvc.perform(delete(BASE_ENDPOINT + "/{uid}", uid128Chars))
                .andExpect(status().isNoContent());

            verify(authUserService, times(1)).deleteAuthUser(uid128Chars);
        }

        @Test
        @DisplayName("Should return 400 when uid exceeds 128 characters")
        void shouldReturn400WhenUidExceeds128Characters() throws Exception {
            // Given: UID with 129 characters
            String uid129Chars = "A".repeat(129);

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(delete(BASE_ENDPOINT + "/{uid}", uid129Chars))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("UID must not exceed 128 characters"));

            // And: Service should NOT be called
            verify(authUserService, never()).deleteAuthUser(any());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given: Service throws NOT_FOUND exception
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(authUserService).deleteAuthUser(TEST_UID);

            // When & Then: Should return 404 NOT_FOUND
            mockMvc.perform(delete(BASE_ENDPOINT + "/{uid}", TEST_UID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));

            verify(authUserService, times(1)).deleteAuthUser(TEST_UID);
        }
    }

    // ========================================
    // PATCH /auth/users/review-preferences
    // ========================================

    @Nested
    @DisplayName("PATCH /auth/users/review-preferences")
    class UpdateReviewPreferences {

        // ========== Success Scenarios (3 tests) - HTTP 200 OK ==========

        @Test
        @DisplayName("Should return 200 OK when updating with PENDING status")
        void shouldReturn200WhenUpdateWithPendingStatus() throws Exception {
            // Given: Valid request with PENDING
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(
                AppReviewStatus.PENDING
            );

            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "John Doe",
                "john@example.com",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                5,
                "2024-01-01T10:00:00+09:00",
                AppReviewStatus.PENDING
            );

            when(authUserService.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                .thenReturn(expectedResponse);

            // When & Then: Should return 200 OK
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uid").value(TEST_UID))
                .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(authUserService, times(1))
                .updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when updating with COMPLETED status")
        void shouldReturn200WhenUpdateWithCompletedStatus() throws Exception {
            // Given: Valid request with COMPLETED
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(
                AppReviewStatus.COMPLETED
            );

            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "John Doe",
                "john@example.com",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                5,
                "2024-01-01T10:00:00+09:00",
                AppReviewStatus.COMPLETED
            );

            when(authUserService.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                .thenReturn(expectedResponse);

            // When & Then: Should return 200 OK
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            verify(authUserService, times(1))
                .updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when updating with PERMANENTLY_DECLINED status")
        void shouldReturn200WhenUpdateWithPermanentlyDeclinedStatus() throws Exception {
            // Given: Valid request with PERMANENTLY_DECLINED
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(
                AppReviewStatus.PERMANENTLY_DECLINED
            );

            AuthUserResponseDTO expectedResponse = new AuthUserResponseDTO(
                TEST_UID,
                "John Doe",
                "john@example.com",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00",
                5,
                "2024-01-01T10:00:00+09:00",
                AppReviewStatus.PERMANENTLY_DECLINED
            );

            when(authUserService.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                .thenReturn(expectedResponse);

            // When & Then: Should return 200 OK
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app_review_status").value("PERMANENTLY_DECLINED"));

            verify(authUserService, times(1))
                .updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        // ========== Validation Failures (3 tests) - HTTP 400 BAD_REQUEST ==========

        @Test
        @DisplayName("Should return 400 when app_review_status is null")
        void shouldReturn400WhenAppReviewStatusIsNull() throws Exception {
            // Given: Request with null app_review_status
            String requestWithNull = "{\"app_review_status\": null}";

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestWithNull))
                .andExpect(status().isBadRequest());

            // And: Service should NOT be called
            verify(authUserService, never()).updateAppReview(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when app_review_status field is missing")
        void shouldReturn400WhenAppReviewStatusFieldMissing() throws Exception {
            // Given: Empty request JSON
            String emptyRequest = "{}";

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyRequest))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).updateAppReview(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when app_review_status is invalid ENUM value")
        void shouldReturn400WhenAppReviewStatusIsInvalidEnum() throws Exception {
            // Given: Request with invalid ENUM value
            String invalidEnumRequest = "{\"app_review_status\": \"INVALID_STATUS\"}";

            // When & Then: Should return 400 BAD_REQUEST (Jackson deserialization error)
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidEnumRequest))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).updateAppReview(any(), any());
        }

        // ========== HTTP Layer Failures (3 tests) ==========

        @Test
        @DisplayName("Should return 400 when request body is completely missing")
        void shouldReturn400WhenRequestBodyMissing() throws Exception {
            // Given: No request body
            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).updateAppReview(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when request body is malformed JSON")
        void shouldReturn400WhenRequestBodyIsMalformedJSON() throws Exception {
            // Given: Malformed JSON
            String malformedJson = "{ invalid json }";

            // When & Then: Should return 400 BAD_REQUEST
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                .andExpect(status().isBadRequest());

            verify(authUserService, never()).updateAppReview(any(), any());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is missing")
        void shouldReturn415WhenContentTypeIsMissing() throws Exception {
            // Given: Valid JSON but no Content-Type header
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(
                AppReviewStatus.COMPLETED
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

            verify(authUserService, never()).updateAppReview(any(), any());
        }

        // ========== Business Logic Failures (2 tests) ==========

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given: Service throws NOT_FOUND exception
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(
                AppReviewStatus.COMPLETED
            );

            when(authUserService.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then: Should return 404 NOT_FOUND
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));

            verify(authUserService, times(1))
                .updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 when internal server error occurs")
        void shouldReturn500WhenInternalServerError() throws Exception {
            // Given: Service throws INTERNAL_SERVER_ERROR exception
            UpdateAppReviewRequestDTO request = new UpdateAppReviewRequestDTO(
                AppReviewStatus.COMPLETED
            );

            when(authUserService.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then: Should return 500 INTERNAL_SERVER_ERROR
            mockMvc.perform(patch(BASE_ENDPOINT + "/review-preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database error"));

            verify(authUserService, times(1))
                .updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }
    }
}
