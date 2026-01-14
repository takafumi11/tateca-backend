package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.constants.BusinessConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request to create a new group")
public record CreateGroupRequestDTO(
        @NotBlank(message = "{validation.group.groupName.required}")
        @Size(min = 1, max = 100, message = "{validation.group.groupName.size}")
        @JsonProperty("group_name")
        @Schema(description = "Name of the group", example = "Team Outing 2024")
        String groupName,

        @NotBlank(message = "{validation.group.hostName.required}")
        @Size(min = 1, max = 50, message = "{validation.group.hostName.size}")
        @JsonProperty("host_name")
        @Schema(description = "Name of the group host", example = "John Doe")
        String hostName,

        @NotNull(message = "{validation.group.participants.required}")
        @Size(min = 1, max = BusinessConstants.MAX_GROUP_PARTICIPANTS, message = "{validation.group.participants.size}")
        @JsonProperty("participants_name")
        @Schema(description = "List of participant names (minimum 1, maximum " + BusinessConstants.MAX_GROUP_PARTICIPANTS + ")", example = "[\"Alice\", \"Bob\", \"Charlie\"]")
        List<@NotBlank(message = "{validation.group.participantName.required}") @Size(min = 1, max = 50, message = "{validation.group.participantName.size}") String> participantsName
) {
}
