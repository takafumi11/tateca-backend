package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.constants.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequestDTO(
        @NotBlank(message = "Group name is required")
        @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
        @JsonProperty("group_name")
        String groupName,

        @NotBlank(message = "Your name is required")
        @Size(min = 1, max = 50, message = "Your name must be between 1 and 50 characters")
        @JsonProperty("host_name")
        String yourName,

        @NotNull(message = "Member names list is required")
        @Size(min = 1, max = BusinessConstants.MAX_GROUP_PARTICIPANTS, message = "Member names must be between 1 and " + BusinessConstants.MAX_GROUP_PARTICIPANTS)
        @JsonProperty("participants_name")
        List<@NotBlank(message = "Member name cannot be blank") @Size(min = 1, max = 50, message = "Member name must be between 1 and 50 characters") String> memberNames
) {
}
