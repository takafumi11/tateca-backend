package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserInfoDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("name") String userName;
    @JsonProperty("currency") CurrencyNameDTO currency;
    @JsonProperty("auth_user")
    AuthUserInfoDTO authUser;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static UserInfoDTO from(UserEntity user) {
        return UserInfoDTO.builder()
                .uuid(user.getUuid().toString())
                .userName(user.getName())
                .currency(CurrencyNameDTO.from(user.getCurrencyName()))
                .authUser(user.getAuthUser() != null ? AuthUserInfoDTO.from(user.getAuthUser()) : null)
                .createdAt(TimeHelper.convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(TimeHelper.convertToTokyoTime(user.getUpdatedAt()))
                .build();
    }
}
