package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User information response")
public record UserResponseDTO(
        @JsonProperty("uuid")
        @Schema(description = "User's unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @JsonProperty("name")
        @Schema(description = "User's display name", example = "John Doe")
        String userName,

        @JsonProperty("auth_user")
        @Schema(description = "Authentication user details")
        AuthUserInfoDTO authUser,

        @JsonProperty("created_at")
        @Schema(description = "User creation timestamp (Tokyo time)", example = "2024-01-01T12:00:00+09:00")
        String createdAt,

        @JsonProperty("updated_at")
        @Schema(description = "User last update timestamp (Tokyo time)", example = "2024-01-15T14:30:00+09:00")
        String updatedAt
) {
    public static UserResponseDTO from(UserEntity user) {
        return new UserResponseDTO(
                user.getUuid().toString(),
                user.getName(),
                user.getAuthUser() != null ? AuthUserInfoDTO.from(user.getAuthUser()) : null,
                TimeHelper.convertToTokyoTime(user.getCreatedAt()),
                TimeHelper.convertToTokyoTime(user.getUpdatedAt())
        );
    }
}
