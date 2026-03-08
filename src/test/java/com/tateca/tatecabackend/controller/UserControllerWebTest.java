package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.exception.domain.ForbiddenException;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.service.UserService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for PATCH /users/{userId}.
 *
 * <p>Note: 401 Unauthorized is defined in the OpenAPI spec but is NOT tested here.
 * Authentication is handled by {@code TatecaAuthenticationFilter} before reaching
 * the Controller. In {@code @WebMvcTest} with {@code TestSecurityConfig}, authentication
 * is bypassed, making 401 scenarios untestable at this layer. 401 coverage is provided
 * by {@code UpdateUserNameScenarioTest} (Req3-AC1+AC2) which runs the full stack.
 */
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("PATCH /users/{userId} — UserController Web Tests")
class UserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static final String ENDPOINT = "/users/{userId}";
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final UserResponseDTO STUB_USER_RESPONSE = new UserResponseDTO(
            USER_ID.toString(),
            "Bob",
            new AuthUserResponseDTO(
                    TestSecurityConfig.TEST_UID,
                    "Auth Name",
                    "bob@example.com",
                    "2024-01-01T12:00:00+09:00",
                    "2024-01-15T14:30:00+09:00",
                    "2024-01-20T10:15:00+09:00",
                    5,
                    null,
                    AppReviewStatus.PENDING
            ),
            "2024-01-01T12:00:00+09:00",
            "2024-01-15T14:30:00+09:00"
    );

    // =================================================================
    // 200 OK — Success
    // =================================================================

    @Nested
    @DisplayName("200 OK — Successful update")
    class Status200 {

        @Test
        @DisplayName("Should return 200 with UserResponse schema when valid request")
        void shouldReturn200WithUserResponseSchema() throws Exception {
            when(userService.updateUserName(anyString(), eq(USER_ID), any(UpdateUserNameRequestDTO.class)))
                    .thenReturn(STUB_USER_RESPONSE);

            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Bob"))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.uuid").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Bob"))
                    .andExpect(jsonPath("$.created_at").exists())
                    .andExpect(jsonPath("$.updated_at").exists())
                    .andExpect(jsonPath("$.auth_user").exists())
                    .andExpect(jsonPath("$.auth_user.uid").value(TestSecurityConfig.TEST_UID))
                    .andExpect(jsonPath("$.auth_user.name").exists())
                    .andExpect(jsonPath("$.auth_user.email").exists())
                    .andExpect(jsonPath("$.auth_user.created_at").exists())
                    .andExpect(jsonPath("$.auth_user.updated_at").exists())
                    .andExpect(jsonPath("$.auth_user.last_login_time").exists())
                    .andExpect(jsonPath("$.auth_user.total_login_count").isNumber())
                    .andExpect(jsonPath("$.auth_user.last_app_review_dialog_shown_at").value(nullValue()))
                    .andExpect(jsonPath("$.auth_user.app_review_status").value("PENDING"));

            verify(userService)
                    .updateUserName(eq(TestSecurityConfig.TEST_UID), eq(USER_ID), any(UpdateUserNameRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 with null auth_user when user has no auth")
        void shouldReturn200WithNullAuthUser() throws Exception {
            UserResponseDTO responseWithoutAuth = new UserResponseDTO(
                    USER_ID.toString(), "Bob", null,
                    "2024-01-01T12:00:00+09:00", "2024-01-15T14:30:00+09:00"
            );
            when(userService.updateUserName(anyString(), eq(USER_ID), any(UpdateUserNameRequestDTO.class)))
                    .thenReturn(responseWithoutAuth);

            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Bob"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.auth_user").value(nullValue()));

            verify(userService)
                    .updateUserName(eq(TestSecurityConfig.TEST_UID), eq(USER_ID), any(UpdateUserNameRequestDTO.class));
        }
    }

    // =================================================================
    // 400 Bad Request — Validation & Malformed JSON
    // =================================================================

    @Nested
    @DisplayName("400 Bad Request — Validation errors")
    class Status400 {

        @Test
        @DisplayName("Should return 400 with VALIDATION.FAILED and errors array when user_name is empty")
        void shouldReturn400WithErrorsArrayWhenEmpty() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].field").exists())
                    .andExpect(jsonPath("$.errors[0].message").exists());

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when user_name is whitespace only")
        void shouldReturn400WhenWhitespaceOnly() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "   "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when user_name key is missing from request")
        void shouldReturn400WhenKeyMissing() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when user_name is null")
        void shouldReturn400WhenNull() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_name\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when user_name exceeds 50 characters")
        void shouldReturn400WhenExceedsMaxLength() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "A".repeat(51)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("VALIDATION.FAILED"));

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 with REQUEST.MALFORMED_JSON when JSON is invalid")
        void shouldReturn400WithMalformedJsonErrorCode() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error_code").value("REQUEST.MALFORMED_JSON"))
                    .andExpect(jsonPath("$.errors").doesNotExist());

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenBodyMissing() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when userId path parameter is not a valid UUID")
        void shouldReturn400WhenUserIdNotUuid() throws Exception {
            mockMvc.perform(patch("/users/{userId}", "not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Bob"))))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }
    }

    // =================================================================
    // 403 Forbidden — Authorization
    // =================================================================

    @Nested
    @DisplayName("403 Forbidden — Authorization errors")
    class Status403 {

        @Test
        @DisplayName("Should return 403 with USER.FORBIDDEN error_code when service throws ForbiddenException")
        void shouldReturn403WithForbiddenErrorCode() throws Exception {
            when(userService.updateUserName(anyString(), eq(USER_ID), any(UpdateUserNameRequestDTO.class)))
                    .thenThrow(new ForbiddenException(ErrorCode.USER_FORBIDDEN));

            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Hacked"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value("You are not allowed to modify this resource"))
                    .andExpect(jsonPath("$.error_code").value("USER.FORBIDDEN"))
                    .andExpect(jsonPath("$.path").value("/users/" + USER_ID))
                    .andExpect(jsonPath("$.errors").doesNotExist());

            verify(userService)
                    .updateUserName(eq(TestSecurityConfig.TEST_UID), eq(USER_ID), any(UpdateUserNameRequestDTO.class));
        }
    }

    // =================================================================
    // 404 Not Found
    // =================================================================

    @Nested
    @DisplayName("404 Not Found — User not found")
    class Status404 {

        @Test
        @DisplayName("Should return 404 with USER.NOT_FOUND error_code when service throws EntityNotFoundException")
        void shouldReturn404WithNotFoundErrorCode() throws Exception {
            when(userService.updateUserName(anyString(), eq(USER_ID), any(UpdateUserNameRequestDTO.class)))
                    .thenThrow(new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Bob"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found"))
                    .andExpect(jsonPath("$.error_code").value("USER.NOT_FOUND"))
                    .andExpect(jsonPath("$.path").value("/users/" + USER_ID))
                    .andExpect(jsonPath("$.errors").doesNotExist());

            verify(userService)
                    .updateUserName(eq(TestSecurityConfig.TEST_UID), eq(USER_ID), any(UpdateUserNameRequestDTO.class));
        }
    }

    // =================================================================
    // 415 Unsupported Media Type
    // =================================================================

    @Nested
    @DisplayName("415 Unsupported Media Type")
    class Status415 {

        @Test
        @DisplayName("Should return 415 with REQUEST.UNSUPPORTED_MEDIA_TYPE when Content-Type is missing")
        void shouldReturn415WhenContentTypeMissing() throws Exception {
            mockMvc.perform(patch(ENDPOINT, USER_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("user_name", "Bob"))))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.status").value(415))
                    .andExpect(jsonPath("$.error_code").value("REQUEST.UNSUPPORTED_MEDIA_TYPE"))
                    .andExpect(jsonPath("$.errors").doesNotExist());

            verify(userService, never()).updateUserName(anyString(), any(), any());
        }
    }
}
