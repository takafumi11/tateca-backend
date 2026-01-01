package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Loan obligation for a specific user")
public record ObligationResponse(
        @JsonProperty("user")
        @Schema(description = "User who owes this amount")
        UserResponseDTO user,

        @JsonProperty("amount")
        @Schema(description = "Amount owed in cents", example = "2500")
        long amount
) {
    public static ObligationResponse from(TransactionObligationEntity obligation) {
        return new ObligationResponse(
                UserResponseDTO.from(obligation.getUser()),
                obligation.getAmount()
        );
    }
}
