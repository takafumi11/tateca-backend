package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Loan obligation specifying user and amount")
public record ObligationRequestDTO(
        @JsonProperty("amount")
        @Schema(description = "Amount owed in cents", example = "2500")
        int amount,

        @JsonProperty("user_uuid")
        @Schema(description = "UUID of the user who owes this amount", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userUuid
) {
}
