package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;

import static com.tateca.tatecabackend.util.TimeHelper.convertToTokyoTime;

public record TransactionHistoryResponse(
        @JsonProperty("transaction_id")
        String id,

        @JsonProperty("transaction_type")
        TransactionType transactionType,

        @JsonProperty("title")
        String title,

        @JsonProperty("amount")
        Integer amount,

        @JsonProperty("exchange_rate")
        ExchangeRateResponse exchangeRateResponse,

        @JsonProperty("date")
        String date
) {
    public static TransactionHistoryResponse from(TransactionHistoryEntity transaction) {
        return new TransactionHistoryResponse(
                transaction.getUuid().toString(),
                transaction.getTransactionType(),
                transaction.getTitle(),
                transaction.getAmount(),
                ExchangeRateResponse.from(transaction.getExchangeRate()),
                convertToTokyoTime(transaction.getTransactionDate())
        );
    }
}
