package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.TransactionHistoryResponse;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Transaction history response containing list of transactions")
public record TransactionHistoryResponseDTO(
        @JsonProperty("transactions_history")
        @Schema(description = "List of transaction history entries")
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
