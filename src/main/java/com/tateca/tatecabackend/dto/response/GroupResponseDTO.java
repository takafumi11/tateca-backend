package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.GroupEntity;
import lombok.Builder;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class GroupResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String name;
    @JsonProperty("join_token") String joinToken;
    @JsonProperty("token_expires") String tokenExpires;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static GroupResponseDTO from(GroupEntity group) {
        return GroupResponseDTO.builder()
                .uuid(group.getUuid().toString())
                .name(group.getName())
                .joinToken(group.getJoinToken().toString())
                .tokenExpires(group.getTokenExpires().toString())
                .createdAt(convertToTokyoTime(group.getCreatedAt()))
                .updatedAt(convertToTokyoTime(group.getUpdatedAt()))
                .build();
    }
}
