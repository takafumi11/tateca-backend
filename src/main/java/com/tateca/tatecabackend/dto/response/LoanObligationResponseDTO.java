package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Loan obligation for a specific user")
public class LoanObligationResponseDTO {
    @JsonProperty("user")
    @Schema(description = "User who owes this amount")
    UserInfoDTO user;

    @JsonProperty("amount")
    @Schema(description = "Amount owed in cents", example = "2500")
    long amount;

    public static LoanObligationResponseDTO from(TransactionObligationEntity obligation) {
        return LoanObligationResponseDTO.builder()
                .user(UserInfoDTO.from(obligation.getUser()))
                .amount(obligation.getAmount())
                .build();
    }
}
