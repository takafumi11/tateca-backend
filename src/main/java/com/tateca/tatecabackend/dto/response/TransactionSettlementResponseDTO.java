package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TransactionSettlementResponseDTO(
   @JsonProperty("transactions_settlement")
   List<TransactionSettlement> transactionsSettlement
) {
    public record TransactionSettlement(
        @JsonProperty("from")
        UserResponseDTO from,

        @JsonProperty("to")
        UserResponseDTO to,

        @JsonProperty("amount")
        long amount
    ) {}
}
