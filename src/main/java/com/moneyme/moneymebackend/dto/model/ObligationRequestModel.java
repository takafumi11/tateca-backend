package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ObligationRequestModel {
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("user_uuid") String userUuid ;
}
