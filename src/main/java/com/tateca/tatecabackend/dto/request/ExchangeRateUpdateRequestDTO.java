package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for updating exchange rates via internal API.
 *
 * @param targetDate Target date for exchange rate data in UTC (YYYY-MM-DD format).
 *                   This should be interpreted as a UTC date regardless of the client's timezone.
 *                   Examples: "2024-01-15", "2023-12-31"
 */
public record ExchangeRateUpdateRequestDTO(
        @JsonProperty("target_date")
        @NotNull(message = "target_date is required")
        LocalDate targetDate
) {
}
