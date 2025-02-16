package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionHistoryResponseDTO {
    @JsonProperty("transaction_id") String id;
    @JsonProperty("transaction_type")
    TransactionType transactionType;
    @JsonProperty("title") String title;
    @JsonProperty("amount") Integer amount;
    @JsonProperty("currency_code") String currencyCode;
    @JsonProperty("date") String date;

    public static TransactionHistoryResponseDTO from(TransactionHistoryEntity transaction) {
        return TransactionHistoryResponseDTO.builder()
                .id(transaction.getUuid().toString())
                .transactionType(transaction.getTransactionType())
                .title(transaction.getTitle())
                .amount(transaction.getAmount())
                .currencyCode(transaction.getExchangeRate().getCurrencyCode())
                .date(transaction.getExchangeRate().getDate().toString())
                .build();
    }
}
