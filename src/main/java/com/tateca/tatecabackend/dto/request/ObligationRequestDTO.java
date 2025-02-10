package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ObligationRequestDTO {
    @JsonProperty("amount") Integer amount;
    @JsonProperty("user_uuid") String userUuid ;
}
