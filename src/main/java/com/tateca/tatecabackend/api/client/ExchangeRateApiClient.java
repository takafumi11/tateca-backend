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
        long startTime = System.currentTimeMillis();
        logger.info("Fetching latest exchange rate from external API");

        try {
            ExchangeRateClientResponse response = httpClient.fetchLatest(apiKey);
            long responseTimeMs = System.currentTimeMillis() - startTime;
            int rateCount = response.conversionRates() != null ? response.conversionRates().size() : 0;

            logger.info("Exchange rate API call succeeded: responseTimeMs={}, rateCount={}, result={}",
                    responseTimeMs, rateCount, response.result());
            return response;
        } catch (Exception e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            logger.warn("Exchange rate API call attempt failed: responseTimeMs={}, error={}",
                    responseTimeMs, e.getMessage());
            throw e; // Resilience4j will retry
        }
    }

    private ExchangeRateClientResponse fetchLatestFallback(Exception e) {
        logger.error("Failed to fetch latest exchange rate after all retries exhausted: {}", e.getMessage(), e);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Exchange rate service unavailable", e);
    }
}
