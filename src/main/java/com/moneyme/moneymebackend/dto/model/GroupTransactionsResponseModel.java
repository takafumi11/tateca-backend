package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTransactionsResponseModel {
    @JsonProperty("from")
    UserResponse from;
    @JsonProperty("to") UserResponse to;
    @JsonProperty("amount") BigDecimal amount;
}
