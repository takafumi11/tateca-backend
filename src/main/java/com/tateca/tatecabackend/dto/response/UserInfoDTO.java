package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "User information")
public class UserInfoDTO {
    @JsonProperty("uuid")
    @Schema(description = "User's unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    String uuid;

    @JsonProperty("name")
    @Schema(description = "User's display name", example = "John Doe")
    String userName;

    @JsonProperty("auth_user")
    @Schema(description = "Authentication user details")
    AuthUserInfoDTO authUser;

    @JsonProperty("created_at")
    @Schema(description = "User creation timestamp (Tokyo time)", example = "2024-01-01T12:00:00+09:00")
    String createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "User last update timestamp (Tokyo time)", example = "2024-01-15T14:30:00+09:00")
    String updatedAt;

    public static UserInfoDTO from(UserEntity user) {
        return UserInfoDTO.builder()
                .uuid(user.getUuid().toString())
                .userName(user.getName())
                .authUser(user.getAuthUser() != null ? AuthUserInfoDTO.from(user.getAuthUser()) : null)
                .createdAt(TimeHelper.convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(TimeHelper.convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
