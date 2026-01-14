package com.tateca.tatecabackend.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standardized error response following RFC 7807 Problem Details pattern.
 *
 * Design principles:
 * - error_code: Machine-readable error identifier for client handling
 * - message: Human-readable localized message for display
 * - status: HTTP status code for API compatibility
 * - timestamp: ISO-8601 timestamp for debugging
 * - path: Request path for troubleshooting (optional)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @JsonProperty("error_code")
    @Schema(description = "Machine-readable error code", example = "USER.NOT_FOUND")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "User not found: 123e4567-e89b-12d3-a456-426614174000")
    private String message;

    @Schema(description = "Timestamp of error occurrence", example = "2025-01-14T12:34:56.789Z")
    private Instant timestamp;

    @Schema(description = "Request path where error occurred", example = "/groups/123/transactions")
    private String path;

    // Legacy constructor for backward compatibility
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = Instant.now();
    }

    // Constructor with error code
    public ErrorResponse(int status, String errorCode, String message) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
