package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.ExchangeRateUpdateRequestDTO;
import com.tateca.tatecabackend.service.ExchangeRateUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Internal system operations (EventBridge + Lambda)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ExchangeRateLambdaController {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateLambdaController.class);

    private final ExchangeRateUpdateService exchangeRateUpdateService;

    @PostMapping
    @Operation(
        summary = "Update exchange rates for a specific date",
        description = """
            Triggered by AWS EventBridge + Lambda to update daily exchange rates.

            **Authentication:** Requires X-API-Key header

            **Flow:**
            1. Lambda receives EventBridge trigger (daily at 00:01 UTC)
            2. Lambda calls this endpoint with target date
            3. System fetches rates from external API
            4. Rates are saved/updated in database

            **Request Body:**
            ```json
            {
              "target_date": "2024-12-26"
            }
            ```
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Exchange rates updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - Check date format (YYYY-MM-DD)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid date format. Expected: YYYY-MM-DD"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid X-API-Key header",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Invalid API Key"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - External API unavailable or database error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 500,
                          "message": "Exchange rate service unavailable for date: 2024-12-26"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Void> updateExchangeRates(
            @Valid @RequestBody ExchangeRateUpdateRequestDTO requestDTO) {

        logger.info("Exchange rate update triggered via HTTP endpoint for date: {}", requestDTO.targetDate());

        int ratesUpdated = exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(requestDTO.targetDate());

        logger.info("Exchange rate update completed successfully. Updated {} rates for date: {}",
                ratesUpdated, requestDTO.targetDate());

        return ResponseEntity.noContent().build();
    }

    // Inner class for OpenAPI documentation
    private static class ErrorResponse {
        public int status;
        public String message;
    }
}
