package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;

import java.time.LocalDate;

/**
 * Service for exchange rate operations (read and write).
 */
public interface ExchangeRateService {
    /**
     * Gets exchange rates for a specific date.
     *
     * @param date The date to retrieve exchange rates for
     * @return Exchange rates for the specified date
     */
    ExchangeRateResponseDTO getExchangeRate(LocalDate date);

    /**
     * Fetches latest exchange rates from external API and stores them in database.
     * The rates are stored with both today's date and tomorrow's date
     * (LocalDate.now() and LocalDate.now().plusDays(1)).
     *
     * Each day's update will:
     * - Create/update today's record (overwrites yesterday's pre-created record)
     * - Create tomorrow's record (new, will be overwritten tomorrow)
     *
     * @return Total number of exchange rate records stored (today + tomorrow)
     */
    int fetchAndStoreLatestExchangeRate();
}
