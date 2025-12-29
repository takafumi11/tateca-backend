package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Settlement information for a group")
public class TransactionsSettlementResponseDTO {
   @JsonProperty("transactions_settlement")
   @Schema(description = "List of settlements needed to balance accounts")
   List<TransactionSettlementResponseDTO> transactionsSettlement;

   @JsonProperty("currency_code")
   @Schema(description = "Currency code for settlements", example = "JPY")
   String currencyCode;
}

