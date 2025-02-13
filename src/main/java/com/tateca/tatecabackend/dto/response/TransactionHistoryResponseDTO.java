package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionEntity;
import com.tateca.tatecabackend.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Data
@Builder
public class TransactionHistoryResponseDTO {
    @JsonProperty("transaction_id") String id;
    @JsonProperty("transaction_type")
    TransactionType transactionType;
    @JsonProperty("title") String title;
    @JsonProperty("amount") Integer amount;
    @JsonProperty("currency_code") String currencyCode;
    @JsonProperty("currency_rate") BigDecimal currencyRate;
    @JsonProperty("date") String date;
    @JsonProperty("payer")
    UserResponseDTO payer;
    @JsonProperty("target_user")
    UserResponseDTO targetUser;
    @JsonProperty("created_at") String createdAt;
  
    public static TransactionHistoryResponseDTO from(TransactionEntity transaction) {
        return TransactionHistoryResponseDTO.builder()
                .id(transaction.getUuid().toString())
                .transactionType(transaction.getTransactionType())
                .title(transaction.getTitle())
                .amount(transaction.getAmount())
                .currencyCode(transaction.getCurrencyCode())
                .currencyRate(transaction.getCurrencyRate())
                .date(convertToTokyoTime(transaction.getDate()))
                .payer(UserResponseDTO.from(transaction.getPayer()))
                // TODO: It can be removed if client doesn't require.
                .targetUser(null)
                .createdAt(convertToTokyoTime(transaction.getCreatedAt()))
                .build();
    }
}
