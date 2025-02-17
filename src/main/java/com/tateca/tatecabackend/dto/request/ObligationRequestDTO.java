package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ObligationRequestDTO {
    @JsonProperty("amount") int amount;
    @JsonProperty("user_uuid")
    UUID userUuid ;
}
