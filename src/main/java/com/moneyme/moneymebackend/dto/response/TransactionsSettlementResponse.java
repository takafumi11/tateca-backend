package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public class TransactionsSettlementResponse {
   @JsonProperty("transactions_settlement")
   List<TransactionSettlementResponseDTO> transactionsSettlement;
}

