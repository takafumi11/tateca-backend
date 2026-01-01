package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ExchangeRateApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateApiClient.class);

    private final String apiKey;
    private final ExchangeRateHttpClient httpClient;

    public ExchangeRateApiClient(
            @Value("${exchange.rate.api-key}") String apiKey,
            ExchangeRateHttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

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
