package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepaymentRequestModel {
    @JsonProperty("title") String title;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("date") String date;
    @JsonProperty("payer_id") String payerId;
    @JsonProperty("recipient_id") String recipientId;
    @JsonProperty("detail") String detail;
}
