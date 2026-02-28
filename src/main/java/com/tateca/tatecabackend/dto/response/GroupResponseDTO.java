package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.GroupResponse;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;

import java.util.List;

public record GroupResponseDTO(
        @JsonProperty("group")
        GroupResponse groupInfo,

        @JsonProperty("users")
        List<UserResponseDTO> users,

        @JsonProperty("transaction_count")
        Long transactionCount
) {
    public static GroupResponseDTO from(List<UserEntity> userEntityList,
                                        GroupEntity groupEntity,
                                        Long transactionCount) {
        return new GroupResponseDTO(
                GroupResponse.from(groupEntity),
                userEntityList.stream()
                        .map(UserResponseDTO::from)
                        .toList(),
                transactionCount
        );
    }
}
