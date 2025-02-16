package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepaymentDetailResponseDTO {
    @JsonProperty("recipient") UserResponseDTO recipient;

    public static RepaymentDetailResponseDTO from(UserEntity recipientEntity) {
        return RepaymentDetailResponseDTO.builder()
                .recipient(UserResponseDTO.from(recipientEntity))
                .build();
    }
}
