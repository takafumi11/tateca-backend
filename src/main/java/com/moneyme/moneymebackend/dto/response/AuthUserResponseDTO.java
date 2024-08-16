package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import lombok.Builder;
import lombok.Data;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
@Data
public class AuthUserResponseDTO {
    @JsonProperty("uid") String uid;
    @JsonProperty("name") String name;
    @JsonProperty("email") String email;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static AuthUserResponseDTO from(AuthUserEntity user) {
        return AuthUserResponseDTO.builder()
                .uid(user.getUid())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
