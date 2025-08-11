package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import lombok.Builder;

@Builder
public class LoanObligationResponseDTO {
    @JsonProperty("user") UserInfoDTO user;
    @JsonProperty("amount") long amount;

    public static LoanObligationResponseDTO from(TransactionObligationEntity obligation) {
        return LoanObligationResponseDTO.builder()
                .user(UserInfoDTO.from(obligation.getUser()))
                .amount(obligation.getAmount())
                .build();
    }
}
