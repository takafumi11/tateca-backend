package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management operations")
public class UserController {
    private final UserService service;

    @PatchMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update user name")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User name updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Empty or blank name",
                        description = "When user_name is null, empty string, or only whitespace",
                        value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "name: User name is required and cannot be blank",
                              "path": "/users/123e4567-e89b-12d3-a456-426614174000"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Name exceeds maximum length",
                        description = "When user_name exceeds 50 characters",
                        value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "name: User name must be between 1 and 50 characters",
                              "path": "/users/123e4567-e89b-12d3-a456-426614174000"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid UUID format",
                        description = "When userId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Invalid format for parameter 'userId': expected UUID",
                              "path": "/users/invalid-uuid"
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
                              "path": "/users/123e4567-e89b-12d3-a456-426614174000"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "error": "Unauthorized",
                          "message": "Unauthorized",
                          "path": "/users/123e4567-e89b-12d3-a456-426614174000"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "error": "Not Found",
                          "message": "User not found",
                          "error_code": "USER.NOT_FOUND",
                          "path": "/users/123e4567-e89b-12d3-a456-426614174000"
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
                          "path": "/users/123e4567-e89b-12d3-a456-426614174000"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Database operation failure",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 500,
                          "error": "Internal Server Error",
                          "message": "Database operation failed",
                          "error_code": "DATABASE.OPERATION_ERROR",
                          "path": "/users/123e4567-e89b-12d3-a456-426614174000"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<UserResponseDTO> updateUserName(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserNameRequestDTO nameRequestDTO
            ) {
        UserResponseDTO responseDTO = service.updateUserName(userId, nameRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
