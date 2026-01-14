package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update user name")
public record UpdateUserNameRequestDTO(
        @JsonProperty("user_name")
        @Schema(description = "User's display name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.user.name.required}")
        @Size(min = 1, max = 50, message = "{validation.user.name.size}")
        String name
) {
}
