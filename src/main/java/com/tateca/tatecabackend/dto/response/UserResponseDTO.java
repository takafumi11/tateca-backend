package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;
import lombok.Builder;
import lombok.Data;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
@Data
public class UserResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String userName;
    @JsonProperty("auth_user")
    AuthUserEntity authUser;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static UserResponseDTO from(UserEntity user) {
        return UserResponseDTO.builder()
                .uuid(user.getUuid().toString())
                .userName(user.getName())
                .authUser(user.getAuthUser())
                .createdAt(TimeHelper.convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(TimeHelper.convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
