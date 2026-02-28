package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.UserEntity;

public record RepaymentResponse(
        @JsonProperty("recipient")
        UserResponseDTO recipient
) {
    public static RepaymentResponse from(UserEntity recipientEntity) {
        return new RepaymentResponse(
                UserResponseDTO.from(recipientEntity)
        );
    }
}
