package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.TransactionHistoryResponse;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;

import java.util.List;

public record TransactionHistoryResponseDTO(
        @JsonProperty("transactions_history")
        List<TransactionHistoryResponse> transactionsHistory
) {
    public static TransactionHistoryResponseDTO buildResponse(List<TransactionHistoryEntity> entityList) {
        List<TransactionHistoryResponse> transactionHistoryResponseList =
                entityList.stream()
                        .map(TransactionHistoryResponse::from)
                        .toList();

        return new TransactionHistoryResponseDTO(transactionHistoryResponseList);
    }
}
