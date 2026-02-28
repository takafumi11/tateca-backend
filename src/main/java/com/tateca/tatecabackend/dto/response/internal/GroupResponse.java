package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.GroupEntity;

import static com.tateca.tatecabackend.util.TimeHelper.convertToTokyoTime;

public record GroupResponse(
        @JsonProperty("uuid")
        String uuid,

        @JsonProperty("name")
        String name,

        @JsonProperty("join_token")
        String joinToken,

        @JsonProperty("token_expires")
        String tokenExpires,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("updated_at")
        String updatedAt
) {
    public static GroupResponse from(GroupEntity group) {
        return new GroupResponse(
                group.getUuid().toString(),
                group.getName(),
                group.getJoinToken().toString(),
                group.getTokenExpires().toString(),
                convertToTokyoTime(group.getCreatedAt()),
                convertToTokyoTime(group.getUpdatedAt())
        );
    }
}
