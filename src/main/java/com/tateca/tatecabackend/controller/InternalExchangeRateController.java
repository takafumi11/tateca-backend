package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.service.InternalExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Internal system operations (EventBridge + Lambda)")
@SecurityRequirement(name = "ApiKeyAuth")
public class InternalExchangeRateController {
    private static final Logger logger = LoggerFactory.getLogger(InternalExchangeRateController.class);

    private final InternalExchangeRateService exchangeRateService;

    @PostMapping
    @Operation(
        summary = "Update exchange rates (fetches latest rates)",
        description = """
            Triggered by AWS EventBridge + Lambda to update daily exchange rates.

            **Authentication:** Requires X-API-Key header

            **Flow:**
            1. Lambda receives EventBridge trigger (daily at 00:01 UTC)
            2. Lambda calls this endpoint
            3. System fetches LATEST rates from external API
            4. Rates are saved/updated in database with current date

            **Request Body:** Not required
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Exchange rates updated successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid X-API-Key header",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
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
                          "message": "Exchange rate service unavailable"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Void> updateExchangeRates() {
        logger.info("Exchange rate update triggered via HTTP endpoint");

        int ratesUpdated = exchangeRateService.fetchAndStoreLatestExchangeRate();

        logger.info("Exchange rate update completed successfully. Stored {} total rates (today + tomorrow)", ratesUpdated);

        return ResponseEntity.noContent().build();
    }
}
