package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
@Data
public class UserResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String userName;
    @JsonProperty("auth_user_uid") AuthUserEntity authUser;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static UserResponseDTO from(UserEntity user) {
        return UserResponseDTO.builder()
                .uuid(user.getUuid().toString())
                .userName(user.getName())
                .authUser(user.getAuthUser())
                .createdAt(convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
