package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;
import lombok.Builder;
import lombok.Data;

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
                .createdAt(TimeHelper.convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(TimeHelper.convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
