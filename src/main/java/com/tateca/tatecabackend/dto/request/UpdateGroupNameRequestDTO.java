package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update group name")
public record UpdateGroupNameRequestDTO(
        @NotBlank(message = "{validation.group.update.groupName.required}")
        @Size(max = 100, message = "{validation.group.update.groupName.size}")
        @JsonProperty("group_name")
        @Schema(description = "New name for the group", example = "Summer Trip 2024")
        String groupName
) {
}
