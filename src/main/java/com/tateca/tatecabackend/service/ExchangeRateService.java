package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;

import java.time.LocalDate;

/**
 * Service for exchange rate query operations (read-only).
 * Handles public API endpoints for retrieving exchange rates.
 */
public interface ExchangeRateService {
    /**
     * Gets exchange rates for a specific date.
     *
     * @param date The date to retrieve exchange rates for
     * @return Exchange rates for the specified date
     */
    ExchangeRateResponseDTO getExchangeRate(LocalDate date);
}
