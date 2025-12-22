package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update group name")
public class UpdateGroupNameRequestDTO {
    @JsonProperty("group_name")
    @Schema(description = "New name for the group", example = "Summer Trip 2024")
    String name;
}
