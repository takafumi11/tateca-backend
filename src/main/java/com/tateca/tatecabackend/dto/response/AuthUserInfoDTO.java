package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.service.util.TimeHelper;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthUserInfoDTO {
    @JsonProperty("uid") String uid;
    @JsonProperty("name") String name;
    @JsonProperty("email") String email;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;
    @JsonProperty("last_login_time") String lastLoginTime;
    @JsonProperty("total_login_count") Integer totalLoginCount;
    @JsonProperty("last_app_review_dialog_shown_at") String lastAppReviewDialogShownAt;
    @JsonProperty("app_review_status") AppReviewStatus appReviewStatus;

    public static AuthUserInfoDTO from(AuthUserEntity user) {
        return AuthUserInfoDTO.builder()
                .uid(user.getUid())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(TimeHelper.convertToTokyoTime(user.getCreatedAt()))
                .updatedAt(TimeHelper.convertToTokyoTime(user.getUpdatedAt()))
                .lastLoginTime(user.getLastLoginTime() != null ? TimeHelper.convertToTokyoTime(user.getLastLoginTime()) : null)
                .totalLoginCount(user.getTotalLoginCount())
                .lastAppReviewDialogShownAt(user.getLastAppReviewDialogShownAt() != null ? TimeHelper.convertToTokyoTime(user.getLastAppReviewDialogShownAt()) : null)
                .appReviewStatus(user.getAppReviewStatus())
                .build();
    }
}
