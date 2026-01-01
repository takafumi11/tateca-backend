package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.tateca.tatecabackend.util.TimeHelper.convertToTokyoTime;

@Schema(description = "Transaction history entry")
public record TransactionHistoryResponse(
        @JsonProperty("transaction_id")
        @Schema(description = "Transaction unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @JsonProperty("transaction_type")
        @Schema(description = "Type of transaction", example = "LOAN")
        TransactionType transactionType,

        @JsonProperty("title")
        @Schema(description = "Transaction title", example = "Dinner at restaurant")
        String title,

        @JsonProperty("amount")
        @Schema(description = "Transaction amount in cents", example = "5000")
        Integer amount,

        @JsonProperty("exchange_rate")
        @Schema(description = "Exchange rate at the time of transaction")
        ExchangeRateResponse exchangeRateResponse,

        @JsonProperty("date")
        @Schema(description = "Transaction date (Tokyo time)", example = "2024-01-15T18:30:00+09:00")
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
