package com.tateca.tatecabackend.service;

import java.time.LocalDate;

/**
 * Service for updating exchange rates from external API.
 */
public interface ExchangeRateUpdateService {

    /**
     * Fetches latest exchange rates from external API and stores them in database.
     * The rates are stored with the current date (LocalDate.now()).
     *
     * @return Number of exchange rate records updated
     */
    int fetchAndStoreLatestExchangeRate();
}
