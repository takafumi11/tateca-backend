package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddMemberRequestDTO(
        @NotBlank(message = "Member name is required")
        @Size(min = 1, max = 50, message = "Member name must be between 1 and 50 characters")
        @JsonProperty("member_name")
        String memberName
) {
}
