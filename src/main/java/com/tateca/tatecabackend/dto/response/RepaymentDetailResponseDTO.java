package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Repayment details")
public record RepaymentDetailResponseDTO(
        @JsonProperty("recipient")
        @Schema(description = "User receiving the repayment")
        UserResponseDTO recipient
) {
    public static RepaymentDetailResponseDTO from(UserEntity recipientEntity) {
        return new RepaymentDetailResponseDTO(
                UserResponseDTO.from(recipientEntity)
        );
    }
}
