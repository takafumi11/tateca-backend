package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
@Schema(description = "Transaction history response containing list of transactions")
public class TransactionsHistoryResponseDTO {
    @JsonProperty("transactions_history")
    @Schema(description = "List of transaction history entries")
    List<TransactionHistoryResponseDTO> transactionsHistory;
  
    public static TransactionsHistoryResponseDTO buildResponse(List<TransactionHistoryEntity> entityList) {
        List<TransactionHistoryResponseDTO> transactionHistoryResponseDTOList = entityList.stream().map(TransactionHistoryResponseDTO::from).toList();

        return TransactionsHistoryResponseDTO.builder()
                .transactionsHistory(transactionHistoryResponseDTOList)
                .build();
    }
}