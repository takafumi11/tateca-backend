package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Loan obligation for a specific user")
public record LoanObligationResponseDTO(
        @JsonProperty("user")
        @Schema(description = "User who owes this amount")
        UserResponseDTO user,

        @JsonProperty("amount")
        @Schema(description = "Amount owed in cents", example = "2500")
        long amount
) {
    public static LoanObligationResponseDTO from(TransactionObligationEntity obligation) {
        return new LoanObligationResponseDTO(
                UserResponseDTO.from(obligation.getUser()),
                obligation.getAmount()
        );
    }
}
