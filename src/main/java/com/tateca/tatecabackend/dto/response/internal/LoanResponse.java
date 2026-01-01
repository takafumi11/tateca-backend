package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Loan details including all obligations")
public record LoanResponse(
        @JsonProperty("obligations")
        @Schema(description = "List of loan obligations showing who owes how much")
        List<ObligationResponse> obligationResponses
) {
    public static LoanResponse from(List<TransactionObligationEntity> transactionObligationEntityList) {
        List<ObligationResponse> obligationResponseList =
                transactionObligationEntityList.stream()
                        .map(ObligationResponse::from)
                        .toList();

        return new LoanResponse(obligationResponseList);
    }
}
