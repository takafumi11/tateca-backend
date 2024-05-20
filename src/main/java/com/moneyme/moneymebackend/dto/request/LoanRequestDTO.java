package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRequestDTO {
    @JsonProperty("title") String title;
    @JsonProperty("amount")
    BigDecimal amount;
    @JsonProperty("date") String date;
    @JsonProperty("payer_id") String payerId;
}
