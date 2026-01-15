package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to add a new member to an existing group")
public record AddMemberRequestDTO(
        @NotBlank(message = "Member name is required")
        @Size(min = 1, max = 50, message = "Member name must be between 1 and 50 characters")
        @JsonProperty("member_name")
        @Schema(description = "Name of the member to add", example = "John Doe")
        String memberName
) {
}
