package com.tateca.tatecabackend.service;

import java.time.LocalDate;

/**
 * Service for updating exchange rates from external API.
 */
public interface ExchangeRateUpdateService {

    /**
     * Fetches exchange rates for a specific date from external API and stores them in database.
     *
     * @param targetDate Target date for exchange rate data (UTC)
     * @return Number of exchange rate records updated
     */
    int fetchAndStoreExchangeRateByDate(LocalDate targetDate);
}
