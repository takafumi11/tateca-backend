package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.AppReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAppReviewRequestDTO {
    @NotNull(message = "show_dialog is required")
    @JsonProperty("show_dialog")
    private Boolean showDialog;
    
    @JsonProperty("app_review_status")
    private AppReviewStatus appReviewStatus;
}