package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class TransactionsHistoryResponse {
    @JsonProperty("transactions_history")
    List<TransactionHistoryResponseDTO> transactionsHistory;
}