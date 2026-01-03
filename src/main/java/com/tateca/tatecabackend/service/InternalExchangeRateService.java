package com.tateca.tatecabackend.service;

/**
 * Service for exchange rate internal operations (write operations).
 * Handles internal API endpoints for updating exchange rates from external sources.
 * Used by Lambda functions triggered by EventBridge.
 */
public interface InternalExchangeRateService {
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
