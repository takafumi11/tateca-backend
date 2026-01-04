package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update group name")
public record UpdateGroupNameRequestDTO(
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        @JsonProperty("group_name")
        @Schema(description = "New name for the group", example = "Summer Trip 2024")
        String groupName
) {
}
