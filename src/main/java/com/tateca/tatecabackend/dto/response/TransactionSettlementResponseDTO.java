package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSettlementResponseDTO {
    @JsonProperty("from")
    UserResponseDTO from;
    @JsonProperty("to")
    UserResponseDTO to;
    @JsonProperty("amount") long amount;
}
