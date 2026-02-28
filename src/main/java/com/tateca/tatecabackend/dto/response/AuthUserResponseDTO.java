package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.util.TimeHelper;

public record AuthUserResponseDTO(
        @JsonProperty("uid")
        String uid,

        @JsonProperty("name")
        String name,

        @JsonProperty("email")
        String email,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("updated_at")
        String updatedAt,

        @JsonProperty("last_login_time")
        String lastLoginTime,

        @JsonProperty("total_login_count")
        Integer totalLoginCount,

        @JsonProperty("last_app_review_dialog_shown_at")
        String lastAppReviewDialogShownAt,

        @JsonProperty("app_review_status")
        AppReviewStatus appReviewStatus
) {
    public static AuthUserResponseDTO from(AuthUserEntity user) {
        return new AuthUserResponseDTO(
                user.getUid(),
                user.getName(),
                user.getEmail(),
                TimeHelper.convertToTokyoTime(user.getCreatedAt()),
                TimeHelper.convertToTokyoTime(user.getUpdatedAt()),
                user.getLastLoginTime() != null
                        ? TimeHelper.convertToTokyoTime(user.getLastLoginTime())
                        : null,
                user.getTotalLoginCount(),
                user.getLastAppReviewDialogShownAt() != null
                        ? TimeHelper.convertToTokyoTime(user.getLastAppReviewDialogShownAt())
                        : null,
                user.getAppReviewStatus()
        );
    }
}
