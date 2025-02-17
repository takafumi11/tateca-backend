package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.model.TransactionType;
import lombok.Data;

import java.util.UUID;

@Data
public class TransactionCreationRequestDTO {
    @JsonProperty("transaction_type")
    private TransactionType transactionType;
    @JsonProperty("title")
    private String title;

    @JsonProperty("amount")
    private int amount;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("date_str")
    private String dateStr;

    @JsonProperty("payer_id")
    private UUID payerId;

    @JsonProperty("loan")
    private LoanCreationRequest loanRequest;

    @JsonProperty("repayment")
    private RepaymentCreationRequest repaymentRequest;
}

