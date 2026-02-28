package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuthUserRequestDTO(
        @JsonProperty("email")
        @NotBlank(message = "Email is required and cannot be blank")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email
) {
}
