package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import lombok.Builder;
import lombok.Data;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
@Data
public class AuthUserResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String name;
    @JsonProperty("email") String email;
    @JsonProperty("uid") String uid;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static AuthUserResponseDTO from(AuthUserEntity user) {
        return AuthUserResponseDTO.builder()
                .uuid(user.getUuid().toString())
                .name(user.getName())
                .email(user.getEmail())
                .uid(user.getUid())
                .createdAt(convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
