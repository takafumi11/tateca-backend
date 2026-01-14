package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.model.AppReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update app review preferences")
public record UpdateAppReviewRequestDTO(
        @JsonProperty("app_review_status")
        @Schema(description = "App review status", example = "COMPLETED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.appReview.status.required}")
        AppReviewStatus appReviewStatus
) {

}
