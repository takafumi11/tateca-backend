package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.AuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserInfoDTO;
import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.service.AuthUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthUserController.class)
@DisplayName("AuthUserController Unit Tests")
class AuthUserControllerUnitTest extends AbstractControllerWebTest {

    @MockitoBean
    private AuthUserService service;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Nested
    @DisplayName("GET /auth/users/{uid}")
    class GetAuthUserTests {

        @Test
        @DisplayName("Should return 200 OK with auth user info when all fields are non-null")
        void shouldReturnOkWithAuthUserInfo() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 5,
                    "2024-01-01T00:00:00+09:00", AppReviewStatus.PENDING
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(uid))
                    .andExpect(jsonPath("$.name").value("Test User"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.last_login_time").value("2024-01-01T00:00:00+09:00"))
                    .andExpect(jsonPath("$.total_login_count").value(5))
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").value("2024-01-01T00:00:00+09:00"))
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 200 OK with null lastLoginTime (DTO ternary operator false branch)")
        void shouldReturnOkWithNullLastLoginTime() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    null, 0,
                    "2024-01-01T00:00:00+09:00", AppReviewStatus.PENDING
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(uid))
                    .andExpect(jsonPath("$.last_login_time").isEmpty())
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").value("2024-01-01T00:00:00+09:00"));

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 200 OK with null lastAppReviewDialogShownAt (DTO ternary operator false branch)")
        void shouldReturnOkWithNullLastAppReviewDialogShownAt() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(uid))
                    .andExpect(jsonPath("$.last_login_time").value("2024-01-01T00:00:00+09:00"))
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").isEmpty());

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 200 OK with both timestamps null (DTO both ternary operators false branch)")
        void shouldReturnOkWithBothTimestampsNull() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    null, 0,
                    null, AppReviewStatus.PENDING
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(uid))
                    .andExpect(jsonPath("$.last_login_time").isEmpty())
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").isEmpty());

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 200 OK with AppReviewStatus PENDING")
        void shouldReturnOkWithAppReviewStatusPending() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 200 OK with AppReviewStatus COMPLETED")
        void shouldReturnOkWithAppReviewStatusCompleted() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.COMPLETED
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 200 OK with AppReviewStatus PERMANENTLY_DECLINED")
        void shouldReturnOkWithAppReviewStatusPermanentlyDeclined() throws Exception {
            // Given
            String uid = "test-uid";
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    uid, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PERMANENTLY_DECLINED
            );

            when(service.getAuthUserInfo(uid)).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("PERMANENTLY_DECLINED"));

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user not found (accessor orElseThrow branch)")
        void shouldReturn404WhenAuthUserNotFound() throws Exception {
            // Given
            String uid = "non-existent-uid";

            when(service.getAuthUserInfo(uid))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found"));

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isNotFound());

            verify(service).getAuthUserInfo(uid);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when database error occurs (accessor catch branch)")
        void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
            // Given
            String uid = "test-uid";

            when(service.getAuthUserInfo(uid))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(get("/auth/users/{uid}", uid))
                    .andExpect(status().isInternalServerError());

            verify(service).getAuthUserInfo(uid);
        }
    }

    @Nested
    @DisplayName("POST /auth/users")
    class CreateAuthUserTests {

        @Test
        @DisplayName("Should return 201 CREATED with created auth user (validateEmail if=false branch)")
        void shouldReturn201WithCreatedAuthUser() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User",
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.name").value("Test User"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.total_login_count").value(1))
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED with name only (email=null)")
        void shouldReturn201WithNameOnly() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", null,
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.name").value("Test User"));

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED with email only (name=null)")
        void shouldReturn201WithEmailOnly() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, null, "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED with empty request body (no @Valid)")
        void shouldReturn201WithEmptyRequestBody() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, null, null,
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value(TEST_UID));

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED with various valid email formats")
        void shouldReturn201WithVariousValidEmails() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "user+tag@domain.co.jp",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User",
                                        "email": "user+tag@domain.co.jp"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("user+tag@domain.co.jp"));

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email already exists (validateEmail if=true branch)")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            // Given
            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User",
                                        "email": "duplicate@example.com"
                                    }
                                    """))
                    .andExpect(status().isConflict());

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when validateEmail throws DataAccessException")
        void shouldReturn500WhenValidateEmailThrowsDataAccessException() throws Exception {
            // Given
            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User",
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when save throws DataAccessException")
        void shouldReturn500WhenSaveThrowsDataAccessException() throws Exception {
            // Given
            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User",
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }

        @Test
        @DisplayName("Should use injected UID from @UId annotation (not from request)")
        void shouldUseInjectedUidFromAnnotation() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/auth/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test User",
                                        "email": "test@example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            // Verify TEST_UID from TestSecurityConfig is used
            verify(service).createAuthUser(eq(TEST_UID), any(AuthUserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /auth/users/{uid}")
    class DeleteAuthUserTests {

        @Test
        @DisplayName("Should return 204 NO_CONTENT when deleting auth user (forEach skip branch)")
        void shouldReturn204WhenDeletingAuthUser() throws Exception {
            // Given
            String uid = "test-uid";
            doNothing().when(service).deleteAuthUser(uid);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", uid))
                    .andExpect(status().isNoContent());

            verify(service).deleteAuthUser(uid);
        }

        @Test
        @DisplayName("Should return 204 NO_CONTENT when user entity references exist (forEach execute branch)")
        void shouldReturn204WhenUserEntityReferencesExist() throws Exception {
            // Given
            String uid = "test-uid";
            doNothing().when(service).deleteAuthUser(uid);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", uid))
                    .andExpect(status().isNoContent());

            verify(service).deleteAuthUser(uid);
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user not found")
        void shouldReturn404WhenAuthUserNotFound() throws Exception {
            // Given
            String uid = "non-existent-uid";
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found"))
                    .when(service).deleteAuthUser(uid);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", uid))
                    .andExpect(status().isNotFound());

            verify(service).deleteAuthUser(uid);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when findByAuthUserUid throws exception")
        void shouldReturn500WhenFindByAuthUserUidThrowsException() throws Exception {
            // Given
            String uid = "test-uid";
            doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"))
                    .when(service).deleteAuthUser(uid);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", uid))
                    .andExpect(status().isInternalServerError());

            verify(service).deleteAuthUser(uid);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when saveAll throws exception")
        void shouldReturn500WhenSaveAllThrowsException() throws Exception {
            // Given
            String uid = "test-uid";
            doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"))
                    .when(service).deleteAuthUser(uid);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", uid))
                    .andExpect(status().isInternalServerError());

            verify(service).deleteAuthUser(uid);
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when deleteById throws DataAccessException")
        void shouldReturn500WhenDeleteByIdThrowsDataAccessException() throws Exception {
            // Given
            String uid = "test-uid";
            doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"))
                    .when(service).deleteAuthUser(uid);

            // When & Then
            mockMvc.perform(delete("/auth/users/{uid}", uid))
                    .andExpect(status().isInternalServerError());

            verify(service).deleteAuthUser(uid);
        }
    }

    @Nested
    @DisplayName("PATCH /auth/users/review-preferences")
    class UpdateReviewPreferencesTests {

        // 4 conditional combinations: showDialog (true/false) x appReviewStatus (null/non-null)

        @Test
        @DisplayName("Should return 200 OK when showDialog=true and appReviewStatus=non-null (both if=true)")
        void shouldReturn200WhenShowDialogTrueAndStatusNonNull() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    "2024-01-01T00:00:00+09:00", AppReviewStatus.COMPLETED
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").value("2024-01-01T00:00:00+09:00"))
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when showDialog=true and appReviewStatus=null (if=true, if=false)")
        void shouldReturn200WhenShowDialogTrueAndStatusNull() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    "2024-01-01T00:00:00+09:00", AppReviewStatus.PENDING
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.last_app_review_dialog_shown_at").value("2024-01-01T00:00:00+09:00"))
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when showDialog=false and appReviewStatus=non-null (if=false, if=true)")
        void shouldReturn200WhenShowDialogFalseAndStatusNonNull() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.COMPLETED
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK when showDialog=false and appReviewStatus=null (both if=false)")
        void shouldReturn200WhenShowDialogFalseAndStatusNull() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value(TEST_UID))
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        // Each enum value tests

        @Test
        @DisplayName("Should return 200 OK with AppReviewStatus PENDING")
        void shouldReturn200WithAppReviewStatusPending() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "PENDING"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("PENDING"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK with AppReviewStatus COMPLETED")
        void shouldReturn200WithAppReviewStatusCompleted() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.COMPLETED
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("COMPLETED"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 200 OK with AppReviewStatus PERMANENTLY_DECLINED")
        void shouldReturn200WithAppReviewStatusPermanentlyDeclined() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PERMANENTLY_DECLINED
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false,
                                        "app_review_status": "PERMANENTLY_DECLINED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app_review_status").value("PERMANENTLY_DECLINED"));

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        // Validation and error cases

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when showDialog field is missing (@Valid fails)")
        void shouldReturn400WhenShowDialogIsMissing() throws Exception {
            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(service, never()).updateAppReview(anyString(), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when showDialog is null (@NotNull fails)")
        void shouldReturn400WhenShowDialogIsNull() throws Exception {
            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": null,
                                        "app_review_status": "COMPLETED"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(service, never()).updateAppReview(anyString(), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when request body is empty (@Valid fails)")
        void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).updateAppReview(anyString(), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when invalid appReviewStatus enum value (JSON parsing error)")
        void shouldReturn400WhenInvalidAppReviewStatus() throws Exception {
            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true,
                                        "app_review_status": "INVALID"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(service, never()).updateAppReview(anyString(), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when auth user not found (accessor orElseThrow branch)")
        void shouldReturn404WhenAuthUserNotFound() throws Exception {
            // Given
            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth User Not Found"));

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isNotFound());

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when findByUid throws DataAccessException")
        void shouldReturn500WhenFindByUidThrowsDataAccessException() throws Exception {
            // Given
            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when save throws DataAccessException")
        void shouldReturn500WhenSaveThrowsDataAccessException() throws Exception {
            // Given
            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": true
                                    }
                                    """))
                    .andExpect(status().isInternalServerError());

            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }

        @Test
        @DisplayName("Should use injected UID from @UId annotation (not from request)")
        void shouldUseInjectedUidFromAnnotation() throws Exception {
            // Given
            AuthUserInfoDTO mockResponse = createAuthUserInfoDTO(
                    TEST_UID, "Test User", "test@example.com",
                    "2024-01-01T00:00:00+09:00", 1,
                    null, AppReviewStatus.PENDING
            );

            when(service.updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(patch("/auth/users/review-preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "show_dialog": false
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Verify TEST_UID from TestSecurityConfig is used
            verify(service).updateAppReview(eq(TEST_UID), any(UpdateAppReviewRequestDTO.class));
        }
    }

    // Helper methods
    private AuthUserInfoDTO createAuthUserInfoDTO(
            String uid, String name, String email,
            String lastLoginTime, Integer totalLoginCount,
            String lastAppReviewDialogShownAt, AppReviewStatus appReviewStatus) {
        return AuthUserInfoDTO.builder()
                .uid(uid)
                .name(name)
                .email(email)
                .createdAt("2024-01-01T00:00:00+09:00")
                .updatedAt("2024-01-01T00:00:00+09:00")
                .lastLoginTime(lastLoginTime)
                .totalLoginCount(totalLoginCount)
                .lastAppReviewDialogShownAt(lastAppReviewDialogShownAt)
                .appReviewStatus(appReviewStatus)
                .build();
    }
}
