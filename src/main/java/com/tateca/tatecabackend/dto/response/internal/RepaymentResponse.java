package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Repayment details")
public record RepaymentResponse(
        @JsonProperty("recipient")
        @Schema(description = "User receiving the repayment")
        UserResponseDTO recipient
) {
    public static RepaymentResponse from(UserEntity recipientEntity) {
        return new RepaymentResponse(
                UserResponseDTO.from(recipientEntity)
        );
    }
}
