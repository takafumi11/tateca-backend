package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.UserEntity;
import lombok.Builder;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class UserResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("user_name") String userName;
    @JsonProperty("email") String email;
    @JsonProperty("auth_user_id") String authUserId;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static UserResponse from(UserEntity user) {
        return UserResponse.builder()
                .uuid(user.getUuid().toString())
                .userName(user.getName())
                .email(user.getEmail())
                .authUserId(user.getAuthUserId())
                .createdAt(convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
