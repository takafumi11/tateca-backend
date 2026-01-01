package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Transaction history response containing list of transactions")
public record TransactionsHistoryResponseDTO(
        @JsonProperty("transactions_history")
        @Schema(description = "List of transaction history entries")
        List<TransactionHistoryResponseDTO> transactionsHistory
) {
    public static TransactionsHistoryResponseDTO buildResponse(List<TransactionHistoryEntity> entityList) {
        List<TransactionHistoryResponseDTO> transactionHistoryResponseDTOList =
                entityList.stream()
                        .map(TransactionHistoryResponseDTO::from)
                        .toList();

        return new TransactionsHistoryResponseDTO(transactionHistoryResponseDTOList);
    }
}
