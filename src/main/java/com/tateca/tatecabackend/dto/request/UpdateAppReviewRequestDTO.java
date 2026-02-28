package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.model.AppReviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppReviewRequestDTO(
        @JsonProperty("app_review_status")
        @NotNull(message = "App review status is required")
        AppReviewStatus appReviewStatus
) {

}
