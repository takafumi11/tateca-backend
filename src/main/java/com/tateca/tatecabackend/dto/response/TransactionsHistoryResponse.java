package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class TransactionsHistoryResponse {
    @JsonProperty("transactions_history")
    List<TransactionHistoryResponseDTO> transactionsHistory;

    public static TransactionsHistoryResponse buildResponse(List<TransactionEntity> entityList) {
        List<TransactionHistoryResponseDTO> transactionHistoryResponseDTOList = entityList.stream().map(TransactionHistoryResponseDTO::from).toList();

        return TransactionsHistoryResponse.builder()
                .transactionsHistory(transactionHistoryResponseDTOList)
                .build();
    }
}