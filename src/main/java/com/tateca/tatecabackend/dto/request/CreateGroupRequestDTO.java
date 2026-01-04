package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request to create a new group")
public record CreateGroupRequestDTO(
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        @JsonProperty("group_name")
        @Schema(description = "Name of the group", example = "Team Outing 2024")
        String groupName,

        @NotBlank(message = "Host name is required")
        @Size(max = 50, message = "Host name must not exceed 50 characters")
        @JsonProperty("host_name")
        @Schema(description = "Name of the group host", example = "John Doe")
        String hostName,

        @NotNull(message = "Participants list is required")
        @Size(min = 1, max = 8, message = "Participants must be between 1 and 8")
        @JsonProperty("participants_name")
        @Schema(description = "List of participant names (minimum 1, maximum 8)", example = "[\"Alice\", \"Bob\", \"Charlie\"]")
        List<@NotBlank(message = "Participant name cannot be blank") @Size(max = 50, message = "Participant name must not exceed 50 characters") String> participantsName
) {
}
