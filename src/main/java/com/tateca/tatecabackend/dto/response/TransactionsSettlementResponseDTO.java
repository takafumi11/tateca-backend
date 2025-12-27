package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Settlement information for a group")
public record TransactionsSettlementResponseDTO(
   @JsonProperty("transactions_settlement")
   @Schema(description = "List of settlements needed to balance accounts")
   List<TransactionSettlementResponseDTO> transactionsSettlement
) {}

