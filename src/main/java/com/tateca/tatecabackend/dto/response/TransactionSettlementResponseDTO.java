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
    UserInfoDTO from;
    @JsonProperty("to")
    UserInfoDTO to;
    @JsonProperty("amount") long amount;
}
