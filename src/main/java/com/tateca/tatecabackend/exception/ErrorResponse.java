package com.tateca.tatecabackend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"timestamp", "status", "error", "message", "path", "errors"})
@Schema(description = "Standard error response following modern REST API best practices")
public class ErrorResponse {
    @Schema(description = "Timestamp when the error occurred in ISO 8601 format", example = "2025-01-14T12:34:56.789Z")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP status reason phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Human-readable error message", example = "Validation failed")
    private String message;

    @Schema(description = "Request path where the error occurred", example = "/groups/123/transactions")
    private String path;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Field-level validation errors (only present for validation failures)")
    private List<FieldError> errors;

    /**
     * Field-level validation error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field-level validation error details")
    public static class FieldError {
        @Schema(description = "Name of the field that failed validation", example = "groupName")
        private String field;

        @Schema(description = "Validation error message", example = "Group name must be between 1 and 100 characters")
        private String message;

        @JsonProperty("rejected_value")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "The value that was rejected", example = "")
        private Object rejectedValue;
    }

    // Maintain backward compatibility with simple constructor
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
