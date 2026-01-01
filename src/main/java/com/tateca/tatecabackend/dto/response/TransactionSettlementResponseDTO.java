package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Settlement information for a group")
public record TransactionSettlementResponseDTO(
   @JsonProperty("transactions_settlement")
   @Schema(description = "List of settlements needed to balance accounts")
   List<TransactionSettlement> transactionsSettlement
) {
    @Schema(description = "Settlement transaction between two users")
    public record TransactionSettlement(
        @JsonProperty("from")
        @Schema(description = "User who owes money")
        UserResponseDTO from,

        @JsonProperty("to")
        @Schema(description = "User who is owed money")
        UserResponseDTO to,

        @JsonProperty("amount")
        @Schema(description = "Amount to be settled in cents", example = "3500")
        long amount
    ) {}
}
