package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Settlement transaction between two users")
public class TransactionSettlementResponseDTO {
    @JsonProperty("from")
    @Schema(description = "User who owes money")
    UserResponseDTO from;

    @JsonProperty("to")
    @Schema(description = "User who is owed money")
    UserResponseDTO to;

    @JsonProperty("amount")
    @Schema(description = "Amount to be settled in cents", example = "3500")
    long amount;
}
