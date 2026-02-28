package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.util.TimeHelper;

public record UserResponseDTO(
        @JsonProperty("uuid")
        String uuid,

        @JsonProperty("name")
        String userName,

        @JsonProperty("auth_user")
        AuthUserResponseDTO authUser,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("updated_at")
        String updatedAt
) {
    public static UserResponseDTO from(UserEntity user) {
        return new UserResponseDTO(
                user.getUuid().toString(),
                user.getName(),
                user.getAuthUser() != null ? AuthUserResponseDTO.from(user.getAuthUser()) : null,
                TimeHelper.convertToTokyoTime(user.getCreatedAt()),
                TimeHelper.convertToTokyoTime(user.getUpdatedAt())
        );
    }
}
