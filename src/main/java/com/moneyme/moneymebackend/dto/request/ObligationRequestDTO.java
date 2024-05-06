package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ObligationRequestDTO {
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("user_uuid") String userUuid ;
}
