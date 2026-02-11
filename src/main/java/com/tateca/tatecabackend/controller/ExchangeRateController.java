package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for exchange rate operations.
 *
 * <p>Provides public API endpoints for querying currency exchange rates.
 * All endpoints require Firebase JWT authentication.
 */
@RestController
@RequestMapping(value = "/exchange-rate", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Currency exchange rate operations")
public class ExchangeRateController {
    private final ExchangeRateService service;

    /**
     * Get exchange rates for a specific date.
     *
     * <p>Returns all active exchange rates for the specified date.
     * If no rates are found, returns an empty array (not 404).
     *
     * @param date The date to query (ISO 8601 format: YYYY-MM-DD)
     * @return Exchange rates for the specified date
     */
    @GetMapping("/{date}")
    @Operation(summary = "Get exchange rates for a specific date", description = "Retrieve exchange rates for a specific date in ISO 8601 format (YYYY-MM-DD). Returns empty array if no rates found for the date.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Exchange rates retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid date format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid date format",
                        description = "When date is not in ISO 8601 format (YYYY-MM-DD) or invalid date (e.g., 2024-13-45)",
                        value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Invalid format for parameter 'date': expected ISO 8601 date (YYYY-MM-DD)",
                              "path": "/exchange-rate/2024-13-45"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Malformed date string",
                        description = "When date parameter is not a valid date string",
                        value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Invalid format for parameter 'date': expected ISO 8601 date (YYYY-MM-DD)",
                              "path": "/exchange-rate/not-a-date"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "error": "Unauthorized",
                          "message": "Unauthorized",
                          "path": "/exchange-rate/2024-01-15"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 500,
                          "error": "Internal Server Error",
                          "message": "Database error occurred",
                          "path": "/exchange-rate/2024-01-15"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ExchangeRateResponseDTO> getExchangeRate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.getExchangeRate(date));
    }
}
