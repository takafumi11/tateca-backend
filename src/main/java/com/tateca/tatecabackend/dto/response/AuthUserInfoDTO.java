package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.util.TimeHelper;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication user information")
public record AuthUserInfoDTO(
        @JsonProperty("uid")
        @Schema(description = "Firebase authentication user ID", example = "firebase-uid-123")
        String uid,

        @JsonProperty("name")
        @Schema(description = "User's name from authentication provider", example = "John Doe")
        String name,

        @JsonProperty("email")
        @Schema(description = "User's email address", example = "john.doe@example.com")
        String email,

        @JsonProperty("created_at")
        @Schema(description = "Account creation timestamp (Tokyo time)", example = "2024-01-01T12:00:00+09:00")
        String createdAt,

        @JsonProperty("updated_at")
        @Schema(description = "Last update timestamp (Tokyo time)", example = "2024-01-15T14:30:00+09:00")
        String updatedAt,

        @JsonProperty("last_login_time")
        @Schema(description = "Last login timestamp (Tokyo time)", example = "2024-01-20T10:15:00+09:00")
        String lastLoginTime,

        @JsonProperty("total_login_count")
        @Schema(description = "Total number of logins", example = "42")
        Integer totalLoginCount,

        @JsonProperty("last_app_review_dialog_shown_at")
        @Schema(description = "Timestamp when app review dialog was last shown (Tokyo time)", example = "2024-01-18T16:45:00+09:00")
        String lastAppReviewDialogShownAt,

        @JsonProperty("app_review_status")
        @Schema(description = "Current app review status")
        AppReviewStatus appReviewStatus
) {
    public static AuthUserInfoDTO from(AuthUserEntity user) {
        return new AuthUserInfoDTO(
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
