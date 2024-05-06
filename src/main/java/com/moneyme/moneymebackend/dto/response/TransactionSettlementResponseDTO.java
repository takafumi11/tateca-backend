package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSettlementResponseDTO {
    @JsonProperty("from")
    UserResponseDTO from;
    @JsonProperty("to")
    UserResponseDTO to;
    @JsonProperty("amount") BigDecimal amount;
}
