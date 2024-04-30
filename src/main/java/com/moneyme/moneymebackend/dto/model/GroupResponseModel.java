package com.moneyme.moneymebackend.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.GroupEntity;
import lombok.Builder;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class GroupResponseModel {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String name;
    @JsonProperty("join_token") String joinToken;
    @JsonProperty("token_expires") String tokenExpires;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static GroupResponseModel from(GroupEntity group) {
        return GroupResponseModel.builder()
                .uuid(group.getUuid().toString())
                .name(group.getName())
                .joinToken(group.getJoinToken().toString())
                .tokenExpires(group.getTokenExpires().toString())
                .createdAt(convertToTokyoTime(group.getCreatedAt()))
                .updatedAt(convertToTokyoTime(group.getUpdatedAt()))
                .build();
    }
}
