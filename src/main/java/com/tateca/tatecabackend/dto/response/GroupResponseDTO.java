package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.GroupResponse;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detailed group information including members and statistics")
public record GroupResponseDTO(
        @JsonProperty("group")
        @Schema(description = "Group information")
        GroupResponse groupInfo,

        @JsonProperty("users")
        @Schema(description = "List of users in the group")
        List<UserResponseDTO> users,

        @JsonProperty("transaction_count")
        @Schema(description = "Total number of transactions in the group", example = "42")
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
