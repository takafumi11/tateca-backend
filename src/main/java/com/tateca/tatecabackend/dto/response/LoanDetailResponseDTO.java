package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Loan details including all obligations")
public record LoanDetailResponseDTO(
        @JsonProperty("obligations")
        @Schema(description = "List of loan obligations showing who owes how much")
        List<LoanObligationResponseDTO> loanObligationResponseDTOs
) {
    public static LoanDetailResponseDTO from(List<TransactionObligationEntity> transactionObligationEntityList) {
        List<LoanObligationResponseDTO> loanObligationResponseDTOList =
                transactionObligationEntityList.stream()
                        .map(LoanObligationResponseDTO::from)
                        .toList();

        return new LoanDetailResponseDTO(loanObligationResponseDTOList);
    }
}
