package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.GroupEntity;
import lombok.Builder;
import lombok.Data;


@Builder
public class GroupResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String name;
    @JsonProperty("join_token") String joinToken;
    @JsonProperty("token_expires") String tokenExpires;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static GroupResponse from(GroupEntity group) {
        return GroupResponse.builder()
                .uuid(group.getUuid().toString())
                .name(group.getName())
                .joinToken(group.getJoinToken().toString())
                .tokenExpires(group.getTokenExpires().toString())
                .createdAt(group.getCreatedAt().toString())
                .updatedAt(group.getUpdatedAt().toString())
                .build();
    }
}
