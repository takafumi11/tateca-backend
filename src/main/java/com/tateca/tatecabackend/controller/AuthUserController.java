package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.service.AuthUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth Users", description = "Authenticated user management operations")
public class AuthUserController {
    private final AuthUserService service;

    @Operation(summary = "Get authenticated user information", description = "Retrieves detailed information about an authenticated user by their UID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found - The specified UID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Auth user not found",
                                              "error_code": "AUTH_USER.NOT_FOUND",
                                              "path": "/auth/users/test-uid-123"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error - Invalid UID format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed",
                                              "path": "/auth/users/test-uid-123"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/{uid}")
    public ResponseEntity<AuthUserResponseDTO> getAuthUser(
            @PathVariable("uid")
            @NotBlank(message = "UID is required and cannot be blank")
            @Size(max = 128, message = "UID must not exceed 128 characters")
            String uid
    ) {
        AuthUserResponseDTO response = service.getAuthUserInfo(uid);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create new authenticated user", description = "Registers a new authenticated user in the system")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error - Invalid request parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Email is null, empty, or whitespace only",
                                            description = "When email is null, empty string, or only whitespace",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "email: Email is required and cannot be blank",
                                                      "path": "/auth/users"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Email exceeds maximum length",
                                            description = "When email exceeds 255 characters",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "email: Email must not exceed 255 characters",
                                                      "path": "/auth/users"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Malformed JSON",
                                            description = "When request body is not valid JSON",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Invalid request body format. Please check the data types and structure.",
                                                      "path": "/auth/users"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Unauthorized",
                                              "path": "/auth/users"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email already exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Email already exists",
                                              "error_code": "AUTH_USER.EMAIL_DUPLICATE",
                                              "path": "/auth/users"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported Media Type - Content-Type header must be application/json",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 415,
                                              "error": "Unsupported Media Type",
                                              "message": "Unsupported Media Type. Content-Type must be application/json",
                                              "path": "/auth/users"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "Database error",
                                              "path": "/auth/users"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponseDTO> createAuthUser(
            @UId String uid,
            @Valid @RequestBody CreateAuthUserRequestDTO request) {
        AuthUserResponseDTO response = service.createAuthUser(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Delete authenticated user", description = "Removes an authenticated user from the system")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found - The specified UID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Auth user not found",
                                              "error_code": "AUTH_USER.NOT_FOUND",
                                              "path": "/auth/users/test-uid-123"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error - Invalid UID format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed",
                                              "path": "/auth/users/test-uid-123"
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUserInfo(
            @PathVariable("uid")
            @NotBlank(message = "UID is required and cannot be blank")
            @Size(max = 128, message = "UID must not exceed 128 characters")
            String uid
    ) {
        service.deleteAuthUser(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update app review preferences", description = "Updates the app review preferences for an authenticated user")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Review preferences updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error - Invalid request parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "App review status is null",
                                            description = "When app_review_status is null",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "appReviewStatus: App review status is required",
                                                      "path": "/auth/users/review-preferences"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid ENUM value",
                                            description = "When app_review_status is not a valid enum value (PENDING, COMPLETED, PERMANENTLY_DECLINED)",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Invalid value for app_review_status: must be PENDING, COMPLETED, or PERMANENTLY_DECLINED",
                                                      "path": "/auth/users/review-preferences"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Malformed JSON",
                                            description = "When request body is not valid JSON",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Invalid request body format. Please check the data types and structure.",
                                                      "path": "/auth/users/review-preferences"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Unauthorized",
                                              "path": "/auth/users/review-preferences"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found - The specified UID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Auth user not found",
                                              "error_code": "AUTH_USER.NOT_FOUND",
                                              "path": "/auth/users/review-preferences"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported Media Type - Content-Type header must be application/json",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 415,
                                              "error": "Unsupported Media Type",
                                              "message": "Unsupported Media Type. Content-Type must be application/json",
                                              "path": "/auth/users/review-preferences"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "Database error",
                                              "path": "/auth/users/review-preferences"
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping(value = "/review-preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponseDTO> updateReviewPreferences(
            @UId String uid,
            @Valid @RequestBody UpdateAppReviewRequestDTO request
    ) {
        AuthUserResponseDTO response = service.updateAppReview(uid, request);
        return ResponseEntity.ok(response);
    }
}
