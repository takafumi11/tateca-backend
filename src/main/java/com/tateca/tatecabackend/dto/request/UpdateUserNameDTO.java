package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update user information")
public class UpdateUserNameDTO {
    @JsonProperty("user_name")
    @Schema(description = "User's display name", example = "John Doe")
    String name;

}
