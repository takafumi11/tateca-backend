package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;

public record UserInfoDTO(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("name") String userName,
        @JsonProperty("currency") CurrencyNameDTO currency,
        @JsonProperty("auth_user") AuthUserInfoDTO authUser,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt
) {
    public static UserInfoDTO from(UserEntity user) {
        return new UserInfoDTO(
                user.getUuid().toString(),
                user.getName(),
                CurrencyNameDTO.from(user.getCurrencyName()),
                user.getAuthUser() != null ? AuthUserInfoDTO.from(user.getAuthUser()) : null,
                TimeHelper.convertToTokyoTime(user.getCreatedAt()),
                TimeHelper.convertToTokyoTime(user.getUpdatedAt())
        );
    }
}
