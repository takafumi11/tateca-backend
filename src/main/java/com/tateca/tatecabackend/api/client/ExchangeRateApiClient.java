package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.scheduler.ExchangeRateScheduler;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;

@Component
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "exchange.rate")
public class ExchangeRateApiClient {
    private String apiKey;
    private String baseUrl;

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateScheduler.class);
    private final RestTemplate restTemplate;

    public ExchangeRateClientResponse fetchLatestExchangeRate() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(1), 2))
                .build();

        Retry retry = Retry.of("fetchLatestExchangeRate", retryConfig);

        String url = baseUrl + "/" + apiKey + "/latest/JPY";

        try {
            return Retry.decorateSupplier(retry, () -> 
                restTemplate.getForObject(url, ExchangeRateClientResponse.class)
            ).get();
        } catch (RestClientException e) {
            logger.error("Failed to fetch exchange rate after retries, detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred in scheduled task, detail: {}", e.getMessage());
            throw e;
        }
    }

    public ExchangeRateClientResponse fetchExchangeRateByDate(LocalDate date) {
        String url = baseUrl + "/" + apiKey + "/history/JPY/"
                + date.getYear() + "/" + date.getMonthValue() + "/" + date.getDayOfMonth();

        try {
            return restTemplate.getForObject(url, ExchangeRateClientResponse.class);
        } catch (RestClientException e) {
            logger.error("Failed to fetch exchange rate, detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred in scheduled task, detail: {}", e.getMessage());
            throw e;
        }
    }
}