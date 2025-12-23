package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.GroupEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
@Schema(description = "Group information")
public class GroupInfoDTO {
    @JsonProperty("uuid")
    @Schema(description = "Group's unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    String uuid;

    @JsonProperty("name")
    @Schema(description = "Group name", example = "Team Outing 2024")
    String name;

    @JsonProperty("join_token")
    @Schema(description = "Invitation token for joining the group", example = "123e4567-e89b-12d3-a456-426614174000")
    String joinToken;

    @JsonProperty("token_expires")
    @Schema(description = "Token expiration date", example = "2024-12-31")
    String tokenExpires;

    @JsonProperty("created_at")
    @Schema(description = "Group creation timestamp (Tokyo time)", example = "2024-01-01T12:00:00+09:00")
    String createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "Group last update timestamp (Tokyo time)", example = "2024-01-15T14:30:00+09:00")
    String updatedAt;

    public static GroupInfoDTO from(GroupEntity group) {
        return GroupInfoDTO.builder()
                .uuid(group.getUuid().toString())
                .name(group.getName())
                .joinToken(group.getJoinToken().toString())
                .tokenExpires(group.getTokenExpires().toString())
                .createdAt(convertToTokyoTime(group.getCreatedAt()))
                .updatedAt(convertToTokyoTime(group.getUpdatedAt()))
                .build();
    }
}
