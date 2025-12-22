package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request to create a new group")
public class CreateGroupRequestDTO {
    @JsonProperty("group_name")
    @Schema(description = "Name of the group", example = "Team Outing 2024")
    String groupName;

    @JsonProperty("host_name")
    @Schema(description = "Name of the group host", example = "John Doe")
    String hostName;

    @JsonProperty("participants_name")
    @Schema(description = "List of participant names", example = "[\"Alice\", \"Bob\", \"Charlie\"]")
    List<String> participantsName;
}
