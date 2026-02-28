package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.service.ExchangeRateService;
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
    public ResponseEntity<ExchangeRateResponseDTO> getExchangeRate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.getExchangeRate(date));
    }
}
