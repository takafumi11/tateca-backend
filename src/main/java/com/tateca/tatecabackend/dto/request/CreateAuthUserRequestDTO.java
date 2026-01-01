package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new authenticated user")
public record CreateAuthUserRequestDTO(
        @JsonProperty("name")
        @Schema(description = "User's display name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "User name is required and cannot be blank")
        @Size(max = 50, message = "User name must not exceed 50 characters")
        String name,

        @JsonProperty("email")
        @Schema(description = "User's email address", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required and cannot be blank")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email
) {
}
