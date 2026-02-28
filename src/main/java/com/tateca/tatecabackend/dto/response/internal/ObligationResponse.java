package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;

public record ObligationResponse(
        @JsonProperty("user")
        UserResponseDTO user,

        @JsonProperty("amount")
        long amount
) {
    public static ObligationResponse from(TransactionObligationEntity obligation) {
        return new ObligationResponse(
                UserResponseDTO.from(obligation.getUser()),
                obligation.getAmount()
        );
    }
}
