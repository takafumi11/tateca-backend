package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;


@AllArgsConstructor
@Schema(description = "Lambda function response containing user authentication information")
public class LambdaResponseDTO {
    @JsonProperty("uid")
    @Schema(description = "User's Firebase authentication ID", example = "firebase-uid-123")
    String uid;
}
