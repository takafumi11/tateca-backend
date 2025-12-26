package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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

    private ExchangeRateClientResponse fetchLatestFallback(Exception e) {
        logger.error("Failed to fetch latest exchange rate after retries, detail: {}", e.getMessage());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Exchange rate service unavailable", e);
    }
}
