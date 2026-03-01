package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserNameRequestDTO(
        @JsonProperty("user_name")
        @NotBlank(message = "User name is required and cannot be blank")
        @Pattern(
                regexp = "^\\s*\\S(?:.{0,48}\\S)?\\s*$",
                message = "User name must be between 1 and 50 characters"
        )
        String name
) {
}
