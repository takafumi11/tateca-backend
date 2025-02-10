package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepaymentRequestDTO {
    @JsonProperty("title") String title;
    @JsonProperty("amount")
    Integer amount;
    @JsonProperty("currency_code") String currencyCode;
    @JsonProperty("currency_rate") BigDecimal currencyRate;
    @JsonProperty("date") String date;
    @JsonProperty("payer_id") String payerId;
    @JsonProperty("recipient_id") String recipientId;
}
