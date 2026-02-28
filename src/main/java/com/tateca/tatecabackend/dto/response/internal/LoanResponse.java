package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;

import java.util.List;

public record LoanResponse(
        @JsonProperty("obligations")
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
