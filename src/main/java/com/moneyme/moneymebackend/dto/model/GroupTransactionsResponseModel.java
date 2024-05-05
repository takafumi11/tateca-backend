package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class GroupBalancesResponseModel {
    @JsonProperty("from") String from;
    @JsonProperty("to") String to;
    @JsonProperty("amount") BigDecimal amount;
}
