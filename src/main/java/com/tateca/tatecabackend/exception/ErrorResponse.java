package com.tateca.tatecabackend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"timestamp", "status", "error", "message", "path", "request_id", "error_code", "errors"})
public class ErrorResponse {
    private String timestamp;

    private int status;

    private String error;

    private String message;

    private String path;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("error_code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FieldError> errors;

    /**
     * Field-level validation error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;

        private String message;

        @JsonProperty("rejected_value")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object rejectedValue;
    }

    // Maintain backward compatibility with simple constructor
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
