package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "exchange.rate")
public class ExchangeRateApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateApiClient.class);

    private String apiKey;
    private final ExchangeRateHttpClient httpClient;

    @Retry(name = "exchangeRateApi", fallbackMethod = "fetchLatestFallback")
    public ExchangeRateClientResponse fetchLatestExchangeRate() {
        logger.debug("Fetching latest exchange rate");
        return httpClient.fetchLatest(apiKey);
    }

    @Retry(name = "exchangeRateApi", fallbackMethod = "fetchByDateFallback")
    public ExchangeRateClientResponse fetchExchangeRateByDate(LocalDate date) {
        logger.debug("Fetching exchange rate for date: {}", date);
        return httpClient.fetchByDate(apiKey, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private ExchangeRateClientResponse fetchLatestFallback(Exception e) {
        logger.error("Failed to fetch latest exchange rate after retries, detail: {}", e.getMessage());
        throw new RuntimeException("Exchange rate service unavailable", e);
    }

    private ExchangeRateClientResponse fetchByDateFallback(LocalDate date, Exception e) {
        logger.error("Failed to fetch exchange rate for date {} after retries, detail: {}", date, e.getMessage());
        throw new RuntimeException("Exchange rate service unavailable for date: " + date, e);
    }
}
