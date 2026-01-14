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

/**
 * Standardized error response following modern REST API best practices.
 *
 * Combines Spring Boot default error response structure with RFC 7807 Problem Details pattern.
 *
 * JSON Structure (Single Error):
 * {
 *   "timestamp": "2025-01-14T12:34:56.789Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "error_code": "USER.NOT_FOUND",
 *   "message": "ユーザーが見つかりません",
 *   "path": "/users/123"
 * }
 *
 * JSON Structure (Validation Errors):
 * {
 *   "timestamp": "2025-01-14T12:34:56.789Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "error_code": "VALIDATION_FAILED",
 *   "message": "入力値が正しくありません",
 *   "path": "/groups",
 *   "errors": [
 *     {
 *       "field": "groupName",
 *       "message": "グループ名を入力してください",
 *       "rejected_value": null
 *     }
 *   ]
 * }
 *
 * Design principles:
 * - timestamp: ISO-8601 timestamp for debugging and audit trails
 * - status: HTTP status code for API compatibility
 * - error: HTTP status reason phrase for human readability
 * - error_code: Machine-readable error identifier for client-side handling
 * - message: Human-readable localized message for display (i18n support)
 * - path: Request path for troubleshooting
 * - errors: Optional structured validation errors for field-level error handling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"timestamp", "status", "error", "error_code", "message", "path", "errors"})
@Schema(description = "Standard error response following modern REST API best practices")
public class ErrorResponse {

    @Schema(description = "Timestamp of error occurrence in ISO-8601 format", example = "2025-01-14T12:34:56.789Z")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "HTTP status reason phrase", example = "Not Found")
    private String error;

    @JsonProperty("error_code")
    @Schema(description = "Machine-readable error code for client-side handling", example = "USER.NOT_FOUND")
    private String errorCode;

    @Schema(description = "Human-readable localized error message", example = "ユーザーが見つかりません")
    private String message;

    @Schema(description = "Request path where error occurred", example = "/users/123")
    private String path;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Detailed field-level validation errors (only present for validation failures)")
    private List<FieldError> errors;

    /**
     * Field-level validation error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field-level validation error")
    public static class FieldError {
        @Schema(description = "Name of the field that failed validation", example = "groupName")
        private String field;

        @Schema(description = "Localized validation error message", example = "グループ名を入力してください")
        private String message;

        @JsonProperty("rejected_value")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "The value that was rejected (null if not provided)", example = "null")
        private Object rejectedValue;
    }
}
