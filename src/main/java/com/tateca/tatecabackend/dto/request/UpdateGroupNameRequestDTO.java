package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupNameRequestDTO(
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        @JsonProperty("group_name")
        String groupName
) {
}
