package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public class TransactionsSettlementResponseDTO {
   @JsonProperty("transactions_settlement")
   List<TransactionSettlementResponseDTO> transactionsSettlement;
}

