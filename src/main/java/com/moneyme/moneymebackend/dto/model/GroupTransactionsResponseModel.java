package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTransactionsResponseModel {
    @JsonProperty("from") String from;
    @JsonProperty("to") String to;
    @JsonProperty("amount") BigDecimal amount;
}
