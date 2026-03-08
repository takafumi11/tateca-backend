package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.service.AuthUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthUserController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("AuthUserController Web Tests")
class AuthUserControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthUserService authUserService;

    private static final String TEST_UID = TestSecurityConfig.TEST_UID;

    private static final AuthUserResponseDTO STUB_RESPONSE = new AuthUserResponseDTO(
            TEST_UID, "John Doe", "john@example.com",
            "2024-01-01T09:00:00+09:00", "2024-01-01T09:00:00+09:00",
            "2024-01-01T09:00:00+09:00", 5, null, AppReviewStatus.PENDING
    );

    // =================================================================
    // GET /auth/users/{uid}
    // =================================================================

    @Nested
    @DisplayName("GET /auth/users/{uid}")
    class GetAuthUser {

        @Nested
        @DisplayName("200 OK")
        class Status200 {

            @Test
            @DisplayName("Should return AuthUserResponse schema with correct fields")
            void shouldReturnAuthUserResponseSchema() throws Exception {
                when(authUserService.getAuthUserInfo(TEST_UID)).thenReturn(STUB_RESPONSE);

                mockMvc.perform(get("/auth/users/{uid}", TEST_UID))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.uid").value(TEST_UID))
                        .andExpect(jsonPath("$.name").value("John Doe"))
                        .andExpect(jsonPath("$.email").value("john@example.com"))
                        .andExpect(jsonPath("$.created_at").exists())
                        .andExpect(jsonPath("$.updated_at").exists())
                        .andExpect(jsonPath("$.last_login_time").exists())
                        .andExpect(jsonPath("$.total_login_count").isNumber())
                        .andExpect(jsonPath("$.app_review_status").value("PENDING"));

                verify(authUserService).getAuthUserInfo(TEST_UID);
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when uid exceeds 128 characters")
            void shouldReturn400WhenUidExceeds128Characters() throws Exception {
                mockMvc.perform(get("/auth/users/{uid}", "A".repeat(129)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.errors").isArray());

                verify(authUserService, never()).getAuthUserInfo(any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 with AUTH_USER.NOT_FOUND error_code")
            void shouldReturn404WithErrorCode() throws Exception {
                when(authUserService.getAuthUserInfo(TEST_UID))
                        .thenThrow(new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND));

                mockMvc.perform(get("/auth/users/{uid}", TEST_UID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("Auth user not found"))
                        .andExpect(jsonPath("$.errors").doesNotExist());

                verify(authUserService).getAuthUserInfo(TEST_UID);
            }
        }
    }

    // =================================================================
    // POST /auth/users
    // =================================================================

    @Nested
    @DisplayName("POST /auth/users")
    class CreateAuthUser {

        @Nested
        @DisplayName("201 Created")
        class Status201 {

            @Test
            @DisplayName("Should return AuthUserResponse schema with correct fields")
            void shouldReturn201WithAuthUserResponseSchema() throws Exception {
                AuthUserResponseDTO createdResponse = new AuthUserResponseDTO(
                        TEST_UID, "", "john@example.com",
                        "2024-01-01T09:00:00+09:00", "2024-01-01T09:00:00+09:00",
                        "2024-01-01T09:00:00+09:00", 1, null, AppReviewStatus.PENDING
                );
                when(authUserService.createAuthUser(eq(TEST_UID), any())).thenReturn(createdResponse);

                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("email", "john@example.com"))))
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.uid").value(TEST_UID))
                        .andExpect(jsonPath("$.email").value("john@example.com"))
                        .andExpect(jsonPath("$.total_login_count").value(1))
                        .andExpect(jsonPath("$.app_review_status").value("PENDING"));

                verify(authUserService).createAuthUser(eq(TEST_UID), any());
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when email is null")
            void shouldReturn400WhenEmailIsNull() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": null}"))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).createAuthUser(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when email is empty")
            void shouldReturn400WhenEmailIsEmpty() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("email", ""))))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).createAuthUser(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when email is whitespace only")
            void shouldReturn400WhenEmailIsWhitespaceOnly() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("email", "   "))))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).createAuthUser(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when email exceeds 255 characters")
            void shouldReturn400WhenEmailExceeds255Characters() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("email", "a".repeat(244) + "@example.com"))))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).createAuthUser(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when email field is missing")
            void shouldReturn400WhenEmailFieldMissing() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).createAuthUser(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when request body is malformed JSON")
            void shouldReturn400WhenMalformedJson() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ invalid json }"))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).createAuthUser(any(), any());
            }
        }

        @Nested
        @DisplayName("409 Conflict")
        class Status409 {

            @Test
            @DisplayName("Should return 409 with AUTH_USER.EMAIL_DUPLICATE error_code")
            void shouldReturn409WithErrorCode() throws Exception {
                when(authUserService.createAuthUser(eq(TEST_UID), any()))
                        .thenThrow(new DuplicateResourceException(ErrorCode.AUTH_USER_EMAIL_DUPLICATE));

                mockMvc.perform(post("/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("email", "existing@example.com"))))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.status").value(409))
                        .andExpect(jsonPath("$.error_code").value("AUTH_USER.EMAIL_DUPLICATE"))
                        .andExpect(jsonPath("$.message").value("Email already exists"))
                        .andExpect(jsonPath("$.errors").doesNotExist());

                verify(authUserService).createAuthUser(eq(TEST_UID), any());
            }
        }

        @Nested
        @DisplayName("415 Unsupported Media Type")
        class Status415 {

            @Test
            @DisplayName("Should return 415 when Content-Type is missing")
            void shouldReturn415WhenContentTypeMissing() throws Exception {
                mockMvc.perform(post("/auth/users")
                                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                        .andExpect(status().isUnsupportedMediaType());

                verify(authUserService, never()).createAuthUser(any(), any());
            }
        }
    }

    // =================================================================
    // DELETE /auth/users/{uid}
    // =================================================================

    @Nested
    @DisplayName("DELETE /auth/users/{uid}")
    class DeleteAuthUser {

        @Nested
        @DisplayName("204 No Content")
        class Status204 {

            @Test
            @DisplayName("Should return 204 and delegate to service")
            void shouldReturn204AndDelegateToService() throws Exception {
                doNothing().when(authUserService).deleteAuthUser(TEST_UID);

                mockMvc.perform(delete("/auth/users/{uid}", TEST_UID))
                        .andExpect(status().isNoContent());

                verify(authUserService).deleteAuthUser(TEST_UID);
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when uid exceeds 128 characters")
            void shouldReturn400WhenUidExceeds128Characters() throws Exception {
                mockMvc.perform(delete("/auth/users/{uid}", "A".repeat(129)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.errors").isArray());

                verify(authUserService, never()).deleteAuthUser(any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 with AUTH_USER.NOT_FOUND error_code")
            void shouldReturn404WithErrorCode() throws Exception {
                doThrow(new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND))
                        .when(authUserService).deleteAuthUser(TEST_UID);

                mockMvc.perform(delete("/auth/users/{uid}", TEST_UID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"))
                        .andExpect(jsonPath("$.errors").doesNotExist());

                verify(authUserService).deleteAuthUser(TEST_UID);
            }
        }
    }

    // =================================================================
    // PATCH /auth/users/review-preferences
    // =================================================================

    @Nested
    @DisplayName("PATCH /auth/users/review-preferences")
    class UpdateReviewPreferences {

        @Nested
        @DisplayName("200 OK")
        class Status200 {

            @Test
            @DisplayName("Should return AuthUserResponse schema with updated status")
            void shouldReturn200WithUpdatedStatus() throws Exception {
                AuthUserResponseDTO updatedResponse = new AuthUserResponseDTO(
                        TEST_UID, "John Doe", "john@example.com",
                        "2024-01-01T09:00:00+09:00", "2024-01-01T09:00:00+09:00",
                        "2024-01-01T09:00:00+09:00", 5,
                        "2024-01-01T10:00:00+09:00", AppReviewStatus.COMPLETED
                );
                when(authUserService.updateAppReview(eq(TEST_UID), any())).thenReturn(updatedResponse);

                mockMvc.perform(patch("/auth/users/review-preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.uid").value(TEST_UID))
                        .andExpect(jsonPath("$.app_review_status").value("COMPLETED"))
                        .andExpect(jsonPath("$.last_app_review_dialog_shown_at").exists());

                verify(authUserService).updateAppReview(eq(TEST_UID), any());
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when app_review_status is null")
            void shouldReturn400WhenStatusIsNull() throws Exception {
                mockMvc.perform(patch("/auth/users/review-preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"app_review_status\": null}"))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).updateAppReview(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when app_review_status field is missing")
            void shouldReturn400WhenStatusFieldMissing() throws Exception {
                mockMvc.perform(patch("/auth/users/review-preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).updateAppReview(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when app_review_status is invalid enum value")
            void shouldReturn400WhenStatusIsInvalidEnum() throws Exception {
                mockMvc.perform(patch("/auth/users/review-preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"app_review_status\": \"INVALID_STATUS\"}"))
                        .andExpect(status().isBadRequest());

                verify(authUserService, never()).updateAppReview(any(), any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 with AUTH_USER.NOT_FOUND error_code")
            void shouldReturn404WithErrorCode() throws Exception {
                when(authUserService.updateAppReview(eq(TEST_UID), any()))
                        .thenThrow(new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND));

                mockMvc.perform(patch("/auth/users/review-preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.error_code").value("AUTH_USER.NOT_FOUND"))
                        .andExpect(jsonPath("$.errors").doesNotExist());

                verify(authUserService).updateAppReview(eq(TEST_UID), any());
            }
        }

        @Nested
        @DisplayName("415 Unsupported Media Type")
        class Status415 {

            @Test
            @DisplayName("Should return 415 when Content-Type is missing")
            void shouldReturn415WhenContentTypeMissing() throws Exception {
                mockMvc.perform(patch("/auth/users/review-preferences")
                                .content(objectMapper.writeValueAsString(Map.of("app_review_status", "COMPLETED"))))
                        .andExpect(status().isUnsupportedMediaType());

                verify(authUserService, never()).updateAppReview(any(), any());
            }
        }
    }
}
