package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ObligationEntity;
import lombok.Builder;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class ObligationResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("user")
    UserResponseDTO user;
    @JsonProperty("amount")
    int amount;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static ObligationResponseDTO from(ObligationEntity obligation) {
        return ObligationResponseDTO.builder()
                .uuid(obligation.getUuid().toString())
                .user(UserResponseDTO.from(obligation.getUser()))
                .amount(obligation.getAmount())
                .createdAt(convertToTokyoTime(obligation.getCreatedAt()))
                .updatedAt(convertToTokyoTime(obligation.getUpdatedAt()))
                .build();
    }
}
