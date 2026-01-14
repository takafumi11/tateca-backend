package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new authenticated user")
public record CreateAuthUserRequestDTO(
        @JsonProperty("email")
        @Schema(description = "User's email address", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.auth.email.required}")
        @Size(max = 255, message = "{validation.auth.email.size}")
        String email
) {
}
