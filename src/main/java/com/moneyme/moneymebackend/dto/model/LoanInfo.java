package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanInfo {
    @JsonProperty("title") String title;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("date") String date;
    @JsonProperty("payer_id") String payerId;
    @JsonProperty("detail") String detail;
}
